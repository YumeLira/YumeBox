use serde_json::{Map as JsonMap, Value as JsonValue};
use sha2::{Digest, Sha256};
use std::collections::HashMap;
use std::path::{Path, PathBuf};

use crate::compiler::schema::{
    field_behavior, DEFAULT_FAKE_IP_FILTER, DEFAULT_FAKE_IP_RANGE, DEFAULT_NAME_SERVERS,
};
use crate::model::{
    FieldBehavior, ListStyle, ParsedKey, PatchModifier, PatchOperations, RunMode, SchemaId,
};

pub fn patch_static_runtime(root: &mut JsonValue, profile_dir: &Path, run_mode: RunMode) {
    let Some(object) = root.as_object_mut() else {
        return;
    };

    object.insert(
        "interface-name".to_string(),
        JsonValue::String(String::new()),
    );
    object.insert("routing-mark".to_string(), JsonValue::from(0));

    if has_non_empty_string(object.get("external-controller"))
        || has_non_empty_string(object.get("external-controller-tls"))
    {
        object.insert(
            "external-ui".to_string(),
            JsonValue::String("./ui".to_string()),
        );
    }

    let profile = ensure_object_field(object, "profile");
    profile.insert("store-selected".to_string(), JsonValue::Bool(true));
    profile.insert("store-fake-ip".to_string(), JsonValue::Bool(true));

    let append_system_dns = object
        .get("clash-for-android")
        .and_then(JsonValue::as_object)
        .and_then(|value| value.get("append-system-dns"))
        .and_then(JsonValue::as_bool)
        .unwrap_or(false);

    let dns_enabled = object
        .get("dns")
        .and_then(JsonValue::as_object)
        .and_then(|value| value.get("enable"))
        .and_then(JsonValue::as_bool)
        .unwrap_or(false);

    if !dns_enabled {
        let dns = ensure_object_field(object, "dns");
        dns.insert("enable".to_string(), JsonValue::Bool(true));
        dns.insert("use-hosts".to_string(), JsonValue::Bool(true));
        dns.insert(
            "default-nameserver".to_string(),
            JsonValue::Array(
                DEFAULT_NAME_SERVERS
                    .iter()
                    .map(|value| JsonValue::String((*value).to_string()))
                    .collect(),
            ),
        );
        dns.insert(
            "nameserver".to_string(),
            JsonValue::Array(
                DEFAULT_NAME_SERVERS
                    .iter()
                    .map(|value| JsonValue::String((*value).to_string()))
                    .collect(),
            ),
        );
        dns.insert(
            "enhanced-mode".to_string(),
            JsonValue::String("fake-ip".to_string()),
        );
        dns.insert(
            "fake-ip-range".to_string(),
            JsonValue::String(DEFAULT_FAKE_IP_RANGE.to_string()),
        );
        dns.insert(
            "fake-ip-filter".to_string(),
            JsonValue::Array(
                DEFAULT_FAKE_IP_FILTER
                    .iter()
                    .map(|value| JsonValue::String((*value).to_string()))
                    .collect(),
            ),
        );
        let clash_for_android = ensure_object_field(object, "clash-for-android");
        clash_for_android.insert("append-system-dns".to_string(), JsonValue::Bool(true));
    }

    if object
        .get("clash-for-android")
        .and_then(JsonValue::as_object)
        .and_then(|value| value.get("append-system-dns"))
        .and_then(JsonValue::as_bool)
        .unwrap_or(append_system_dns)
    {
        let dns = ensure_object_field(object, "dns");
        let nameserver = ensure_array_field(dns, "nameserver");
        if !nameserver
            .iter()
            .any(|item| item.as_str() == Some("system://"))
        {
            nameserver.push(JsonValue::String("system://".to_string()));
        }
    }

    // An override may force `dns.enable: true` onto a profile that carries no nameservers (the
    // built-in Tun override does exactly this when the subscription has no `dns:` block). mihomo
    // hard-fails on "DNS enabled but NameServer empty", killing the core at parse time — backfill
    // the defaults so an enabled-but-empty DNS block always resolves.
    let dns_enabled_without_nameserver = object
        .get("dns")
        .and_then(JsonValue::as_object)
        .map(|dns| {
            dns.get("enable")
                .and_then(JsonValue::as_bool)
                .unwrap_or(false)
                && dns
                    .get("nameserver")
                    .and_then(JsonValue::as_array)
                    .map(Vec::is_empty)
                    .unwrap_or(true)
        })
        .unwrap_or(false);
    if dns_enabled_without_nameserver {
        let dns = ensure_object_field(object, "dns");
        let defaults = || {
            JsonValue::Array(
                DEFAULT_NAME_SERVERS
                    .iter()
                    .map(|value| JsonValue::String((*value).to_string()))
                    .collect(),
            )
        };
        dns.insert("nameserver".to_string(), defaults());
        let default_nameserver_missing = dns
            .get("default-nameserver")
            .and_then(JsonValue::as_array)
            .map(Vec::is_empty)
            .unwrap_or(true);
        if default_nameserver_missing {
            dns.insert("default-nameserver".to_string(), defaults());
        }
    }

    // In the VpnService path the TUN is attached at runtime via a file descriptor; a config-provided
    // tun block must never open its own /dev/net/tun (it would fail on non-root and fight the
    // fd-based listener). TPROXY also needs TUN off (mihomo refuses to program iptables while a tun is
    // up). So force any tun block off — EXCEPT in tun mode, where the compiled tun block is
    // authoritative and the core opens its own kernel device (auto-route / auto-detect-interface) in
    // the root domain.
    if run_mode != RunMode::Tun {
        if let Some(tun) = object.get_mut("tun").and_then(JsonValue::as_object_mut) {
            tun.insert("enable".to_string(), JsonValue::Bool(false));
            tun.insert("auto-route".to_string(), JsonValue::Bool(false));
            tun.insert("auto-detect-interface".to_string(), JsonValue::Bool(false));
        }
    }

    patch_listeners(object);
    patch_providers(object, profile_dir);
}

fn patch_listeners(object: &mut JsonMap<String, JsonValue>) {
    let Some(listeners) = object
        .get_mut("listeners")
        .and_then(JsonValue::as_array_mut)
    else {
        return;
    };
    listeners.retain(|listener| {
        listener
            .as_object()
            .and_then(|value| value.get("type"))
            .and_then(JsonValue::as_str)
            .map(|kind| !matches!(kind, "tproxy" | "redir" | "tun"))
            .unwrap_or(true)
    });
}

pub fn patch_providers(object: &mut JsonMap<String, JsonValue>, profile_dir: &Path) {
    for (field, prefix) in [("proxy-providers", "proxies"), ("rule-providers", "rules")] {
        let Some(providers) = object.get_mut(field).and_then(JsonValue::as_object_mut) else {
            continue;
        };
        for (_, provider) in providers.iter_mut() {
            let Some(provider_object) = provider.as_object_mut() else {
                continue;
            };
            let extension = provider_extension(provider_object, prefix);
            if let Some(path) = provider_object.get("path").and_then(JsonValue::as_str) {
                if !path.trim().is_empty() {
                    let normalized_path =
                        normalize_provider_path(path, profile_dir, prefix, extension);
                    provider_object.insert("path".to_string(), JsonValue::String(normalized_path));
                    continue;
                }
            }
            if provider_object
                .get("type")
                .and_then(JsonValue::as_str)
                .map(|value| value.eq_ignore_ascii_case("inline"))
                .unwrap_or(false)
            {
                continue;
            }
            let Some(url) = provider_object.get("url").and_then(JsonValue::as_str) else {
                continue;
            };
            let mut hasher = Sha256::new();
            hasher.update(url.as_bytes());
            let hash: String = hasher.finalize().iter().map(|byte| format!("{byte:02x}")).collect();
            provider_object.insert(
                "path".to_string(),
                JsonValue::String(profile_provider_path(
                    profile_dir,
                    prefix,
                    &Path::new(&format!("{hash}.{extension}")),
                )),
            );
        }
    }
}

pub fn validate_provider_paths(
    object: &JsonMap<String, JsonValue>,
    profile_dir: &Path,
) -> Result<(), String> {
    let runtime_home = runtime_home_dir(profile_dir);
    for (field, prefix) in [("proxy-providers", "proxies"), ("rule-providers", "rules")] {
        let Some(providers) = object.get(field).and_then(JsonValue::as_object) else {
            continue;
        };
        let expected_base = normalize_path(profile_dir.join("providers").join(prefix));
        for (name, provider) in providers {
            let provider_object = provider
                .as_object()
                .ok_or_else(|| format!("{field}.{name} must be an object"))?;
            let is_inline = provider_object
                .get("type")
                .and_then(JsonValue::as_str)
                .map(|value| value.eq_ignore_ascii_case("inline"))
                .unwrap_or(false);
            let path = provider_object
                .get("path")
                .and_then(JsonValue::as_str)
                .map(str::trim)
                .filter(|value| !value.is_empty());
            if is_inline && path.is_none() {
                continue;
            }
            let path = path.ok_or_else(|| format!("{field}.{name} missing normalized path"))?;
            let candidate = Path::new(path);
            let resolved_candidate = if candidate.is_absolute() {
                candidate.to_path_buf()
            } else {
                normalize_path(runtime_home.join(candidate))
            };
            if candidate.is_absolute() || !resolved_candidate.starts_with(&expected_base) {
                return Err(format!(
                    "{field}.{name} path escaped profile scope: {path} (expected under {})",
                    expected_base.to_string_lossy()
                ));
            }
        }
    }
    Ok(())
}

pub fn apply_override_document(target: &mut JsonValue, patch: &JsonValue) {
    let Some(patch_object) = patch.as_object() else {
        *target = clone_raw_value(patch);
        return;
    };

    if !target.is_object() {
        *target = JsonValue::Object(JsonMap::new());
    }

    let target_object = target.as_object_mut().expect("root target object");
    for (base_key, operations) in group_patch_keys(patch_object) {
        match field_behavior(SchemaId::Root, &base_key) {
            Some(behavior) => apply_field(target_object, &base_key, behavior, operations),
            None => apply_generic_field(target_object, &base_key, operations),
        }
    }
}

fn apply_field(
    target_object: &mut JsonMap<String, JsonValue>,
    base_key: &str,
    behavior: FieldBehavior,
    operations: PatchOperations<'_>,
) {
    if let Some(force) = operations.force {
        target_object.insert(base_key.to_string(), clone_raw_value(force));
        return;
    }

    match behavior {
        FieldBehavior::Scalar => {
            if let Some(replace) = operations.replace {
                target_object.insert(base_key.to_string(), clone_raw_value(replace));
            }
        }
        FieldBehavior::List(style) => apply_list_field(target_object, base_key, style, operations),
        FieldBehavior::Map => {
            if let Some(merge) = operations.merge {
                let entry = target_object
                    .entry(base_key.to_string())
                    .or_insert_with(|| JsonValue::Object(JsonMap::new()));
                merge_raw_map(entry, merge);
            }
            if let Some(replace) = operations.replace {
                target_object.insert(base_key.to_string(), clone_raw_value(replace));
            }
        }
        FieldBehavior::Object(schema) => {
            if let Some(replace) = operations.replace {
                let entry = target_object
                    .entry(base_key.to_string())
                    .or_insert_with(|| JsonValue::Object(JsonMap::new()));
                if replace.is_object() {
                    apply_nested_object_with_schema(entry, replace, schema);
                } else {
                    *entry = clone_raw_value(replace);
                }
            }
            if let Some(merge) = operations.merge {
                let entry = target_object
                    .entry(base_key.to_string())
                    .or_insert_with(|| JsonValue::Object(JsonMap::new()));
                merge_raw_map(entry, merge);
            }
        }
        FieldBehavior::Rules => {
            apply_list_field(target_object, base_key, ListStyle::Plain, operations)
        }
    }
}

fn apply_generic_field(
    target_object: &mut JsonMap<String, JsonValue>,
    base_key: &str,
    operations: PatchOperations<'_>,
) {
    if let Some(force) = operations.force {
        target_object.insert(base_key.to_string(), clone_raw_value(force));
        return;
    }
    if let Some(merge) = operations.merge {
        let entry = target_object
            .entry(base_key.to_string())
            .or_insert_with(|| JsonValue::Object(JsonMap::new()));
        merge_raw_map(entry, merge);
    }
    if let Some(replace) = operations.replace {
        if replace.is_object() {
            let entry = target_object
                .entry(base_key.to_string())
                .or_insert_with(|| JsonValue::Object(JsonMap::new()));
            apply_override_document(entry, replace);
        } else {
            target_object.insert(base_key.to_string(), clone_raw_value(replace));
        }
    }
    if operations.start.is_some() || operations.end.is_some() {
        let mut items = Vec::<JsonValue>::new();
        if let Some(start) = operations.start {
            items.extend(collect_array_items(start));
        }
        items.append(&mut take_array_field(target_object, base_key));
        if let Some(end) = operations.end {
            items.extend(collect_array_items(end));
        }
        target_object.insert(base_key.to_string(), JsonValue::Array(items));
    }
}

fn apply_list_field(
    target_object: &mut JsonMap<String, JsonValue>,
    base_key: &str,
    style: ListStyle,
    operations: PatchOperations<'_>,
) {
    if let Some(force) = operations.force {
        target_object.insert(base_key.to_string(), clone_raw_value(force));
        return;
    }

    let mut items = Vec::new();
    if let Some(start) = operations.start {
        items.extend(collect_array_items(start));
    }
    if let Some(replace) = operations.replace {
        items.extend(collect_array_items(replace));
    } else {
        items.append(&mut take_array_field(target_object, base_key));
    }
    if let Some(end) = operations.end {
        items.extend(collect_array_items(end));
    }
    if matches!(style, ListStyle::NamedObjects) {
        items = dedup_named_items(items);
    }
    target_object.insert(base_key.to_string(), JsonValue::Array(items));
}

fn apply_nested_object_with_schema(target: &mut JsonValue, patch: &JsonValue, schema: SchemaId) {
    let Some(patch_object) = patch.as_object() else {
        *target = clone_raw_value(patch);
        return;
    };
    if !target.is_object() {
        *target = JsonValue::Object(JsonMap::new());
    }
    let target_object = target.as_object_mut().expect("nested target object");
    for (base_key, operations) in group_patch_keys(patch_object) {
        match field_behavior(schema, &base_key) {
            Some(behavior) => apply_field(target_object, &base_key, behavior, operations),
            None => apply_generic_field(target_object, &base_key, operations),
        }
    }
}

fn group_patch_keys<'a>(map: &'a JsonMap<String, JsonValue>) -> Vec<(String, PatchOperations<'a>)> {
    let mut grouped = Vec::<(String, PatchOperations<'a>)>::with_capacity(map.len());
    let mut positions = HashMap::<String, usize>::with_capacity(map.len());
    for (key, value) in map {
        let parsed = parse_modifier_key(key);
        let index = if let Some(index) = positions.get(parsed.base) {
            *index
        } else {
            let index = grouped.len();
            let base_key = parsed.base.to_string();
            positions.insert(base_key.clone(), index);
            grouped.push((base_key, PatchOperations::default()));
            index
        };
        let operations = &mut grouped[index].1;
        match parsed.modifier {
            PatchModifier::Replace => operations.replace = Some(value),
            PatchModifier::Start => operations.start = Some(value),
            PatchModifier::End => operations.end = Some(value),
            PatchModifier::Merge => operations.merge = Some(value),
            PatchModifier::Force => operations.force = Some(value),
        }
    }
    grouped
}

fn parse_modifier_key(key: &str) -> ParsedKey<'_> {
    if let Some(inner) = literal_key_inner(key) {
        return ParsedKey {
            base: inner,
            modifier: PatchModifier::Replace,
        };
    }

    for (suffix, modifier) in [
        ("-start", PatchModifier::Start),
        ("-end", PatchModifier::End),
        ("-merge", PatchModifier::Merge),
        ("-force", PatchModifier::Force),
    ] {
        if let Some(base) = key.strip_suffix(suffix) {
            return ParsedKey { base, modifier };
        }
    }

    ParsedKey {
        base: key,
        modifier: PatchModifier::Replace,
    }
}

fn merge_raw_map(target: &mut JsonValue, patch: &JsonValue) {
    let Some(patch_object) = patch.as_object() else {
        *target = clone_raw_value(patch);
        return;
    };

    if !target.is_object() {
        *target = JsonValue::Object(JsonMap::new());
    }

    let target_object = target.as_object_mut().expect("raw map target");
    for (key, value) in patch_object {
        let key = unescape_literal_key(key);
        match (target_object.get_mut(&key), value.as_object()) {
            (Some(existing), Some(_)) if existing.is_object() => merge_raw_map(existing, value),
            _ => {
                target_object.insert(key, clone_raw_value(value));
            }
        }
    }
}

pub fn clone_raw_value(value: &JsonValue) -> JsonValue {
    match value {
        JsonValue::Object(object) => JsonValue::Object(
            object
                .iter()
                .map(|(key, value)| (unescape_literal_key(key), clone_raw_value(value)))
                .collect(),
        ),
        JsonValue::Array(items) => JsonValue::Array(items.iter().map(clone_raw_value).collect()),
        _ => value.clone(),
    }
}

fn collect_array_items(value: &JsonValue) -> Vec<JsonValue> {
    match value {
        JsonValue::Null => Vec::new(),
        JsonValue::Array(items) => items.iter().map(clone_raw_value).collect(),
        _ => vec![clone_raw_value(value)],
    }
}

fn take_array_field(target_object: &mut JsonMap<String, JsonValue>, key: &str) -> Vec<JsonValue> {
    match target_object.remove(key) {
        Some(JsonValue::Array(items)) => items,
        _ => Vec::new(),
    }
}

fn dedup_named_items(items: Vec<JsonValue>) -> Vec<JsonValue> {
    let mut out = Vec::<JsonValue>::new();
    let mut seen = std::collections::HashSet::<String>::new();

    for item in items.into_iter().rev() {
        let Some(name) = extract_name(&item) else {
            out.push(item);
            continue;
        };
        if seen.insert(name) {
            out.push(item);
        }
    }

    out.reverse();
    out
}

fn extract_name(value: &JsonValue) -> Option<String> {
    value
        .as_object()
        .and_then(|map| map.get("name"))
        .and_then(JsonValue::as_str)
        .map(|value| value.trim().to_string())
        .filter(|value| !value.is_empty())
}

fn literal_key_inner(key: &str) -> Option<&str> {
    key.strip_prefix('<')
        .and_then(|value| value.strip_suffix('>'))
}

fn unescape_literal_key(key: &str) -> String {
    literal_key_inner(key).unwrap_or(key).to_string()
}

fn provider_extension(provider: &JsonMap<String, JsonValue>, prefix: &str) -> &'static str {
    if prefix == "rules"
        && provider
            .get("format")
            .and_then(JsonValue::as_str)
            .map(|value| value.eq_ignore_ascii_case("mrs"))
            .unwrap_or(false)
    {
        return "mrs";
    }
    "yaml"
}

fn normalize_provider_path(
    path: &str,
    profile_dir: &Path,
    prefix: &str,
    extension: &str,
) -> String {
    let raw = Path::new(path);
    let profile_base = profile_dir.join("providers").join(prefix);
    if raw.is_absolute() && raw.starts_with(&profile_base) {
        return relative_to_runtime_home(profile_dir, raw);
    }
    let cleaned = if raw.is_absolute() {
        raw.file_name().map(PathBuf::from).unwrap_or_default()
    } else {
        trim_provider_prefix(clean_relative_path(raw))
    };
    let trimmed = trim_provider_prefix(cleaned);
    let normalized = ensure_provider_extension(trimmed, extension);
    profile_provider_path(profile_dir, prefix, &normalized)
}

fn profile_provider_path(profile_dir: &Path, prefix: &str, relative: &Path) -> String {
    let tail = if relative.as_os_str().is_empty() {
        PathBuf::from("provider.yaml")
    } else {
        relative.to_path_buf()
    };
    let provider_path = profile_dir.join("providers").join(prefix).join(tail);
    relative_to_runtime_home(profile_dir, &provider_path)
}

fn relative_to_runtime_home(profile_dir: &Path, path: &Path) -> String {
    let runtime_home = runtime_home_dir(profile_dir);
    relative_path_from(&normalize_path(path), &normalize_path(runtime_home))
        .unwrap_or_else(|| path.to_path_buf())
        .to_string_lossy()
        .replace('\\', "/")
        .to_string()
}

fn trim_provider_prefix(mut path: PathBuf) -> PathBuf {
    loop {
        let first = match path.iter().next() {
            Some(value) => value.to_string_lossy(),
            None => break,
        };
        if matches!(first.as_ref(), "providers" | "provider" | "clash") {
            if let Ok(rest) = path.strip_prefix(Path::new(first.as_ref())) {
                path = rest.to_path_buf();
                continue;
            }
        }
        if matches!(first.as_ref(), "ruleset" | "rules" | "proxies") {
            if let Ok(rest) = path.strip_prefix(Path::new(first.as_ref())) {
                path = rest.to_path_buf();
                continue;
            }
        }
        break;
    }
    path
}

fn ensure_provider_extension(path: PathBuf, extension: &str) -> PathBuf {
    if path.as_os_str().is_empty() {
        return PathBuf::from(format!("provider.{extension}"));
    }
    if path.extension().is_some() {
        return path;
    }
    PathBuf::from(format!("{}.{}", path.to_string_lossy(), extension))
}

fn clean_relative_path(path: &Path) -> PathBuf {
    use std::path::Component;
    let mut cleaned = PathBuf::new();
    for component in path.components() {
        match component {
            Component::CurDir => continue,
            Component::ParentDir => {
                cleaned.pop();
            }
            Component::Normal(part) => cleaned.push(part),
            _ => {}
        }
    }
    cleaned
}

fn runtime_home_dir(profile_dir: &Path) -> PathBuf {
    profile_dir
        .parent()
        .and_then(Path::parent)
        .map(|files_dir| files_dir.join("mihomo"))
        .unwrap_or_else(|| profile_dir.to_path_buf())
}

fn relative_path_from(path: &Path, base: &Path) -> Option<PathBuf> {
    use std::path::Component;
    let path_components = path.components().collect::<Vec<_>>();
    let base_components = base.components().collect::<Vec<_>>();

    let mut common_count = 0;
    while common_count < path_components.len()
        && common_count < base_components.len()
        && path_components[common_count] == base_components[common_count]
    {
        common_count += 1;
    }

    if common_count == 0 {
        return None;
    }

    let mut result = PathBuf::new();
    for component in &base_components[common_count..] {
        if matches!(component, Component::Normal(_)) {
            result.push("..");
        }
    }
    for component in &path_components[common_count..] {
        result.push(component.as_os_str());
    }
    Some(result)
}

fn normalize_path(path: impl AsRef<Path>) -> PathBuf {
    use std::path::Component;
    let mut normalized = PathBuf::new();
    for component in path.as_ref().components() {
        match component {
            Component::CurDir => continue,
            Component::ParentDir => {
                normalized.pop();
            }
            Component::Normal(part) => normalized.push(part),
            Component::RootDir | Component::Prefix(_) => normalized.push(component.as_os_str()),
        }
    }
    normalized
}

fn ensure_object_field<'a>(
    object: &'a mut JsonMap<String, JsonValue>,
    key: &str,
) -> &'a mut JsonMap<String, JsonValue> {
    if !object.get(key).map(JsonValue::is_object).unwrap_or(false) {
        object.insert(key.to_string(), JsonValue::Object(JsonMap::new()));
    }
    object
        .get_mut(key)
        .and_then(JsonValue::as_object_mut)
        .expect("object field")
}

fn ensure_array_field<'a>(
    object: &'a mut JsonMap<String, JsonValue>,
    key: &str,
) -> &'a mut Vec<JsonValue> {
    if !object.get(key).map(JsonValue::is_array).unwrap_or(false) {
        object.insert(key.to_string(), JsonValue::Array(Vec::new()));
    }
    object
        .get_mut(key)
        .and_then(JsonValue::as_array_mut)
        .expect("array field")
}

fn has_non_empty_string(value: Option<&JsonValue>) -> bool {
    value
        .and_then(JsonValue::as_str)
        .map(|value| !value.trim().is_empty())
        .unwrap_or(false)
}
