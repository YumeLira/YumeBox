/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025 - Present
 *
 */

package com.github.yumelira.yumebox.feature.meta.presentation.util

import com.github.yumelira.yumebox.core.util.YamlCodec
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private const val OFFICIAL_MRS_RULE_PROVIDER_INTERVAL = 86400
private const val OFFICIAL_MRS_URL_TEST_INTERVAL = 300
private const val OFFICIAL_MRS_URL_TEST_URL = "https://www.gstatic.com/generate_204"
private const val OFFICIAL_MRS_EXCLUDE_FILTER =
    "(?i)GB|Traffic|Expire|Premium|频道|订阅|ISP|流量|到期|重置"
private const val OFFICIAL_MRS_POPULAR_REGION_EXCLUDE_FILTER =
    "(?i)(香港|HK|Hong Kong|🇭🇰|台湾|TW|Taiwan|🇹🇼|日本|JP|Japan|东京|大阪|🇯🇵|新加坡|SG|Singapore|狮城|🇸🇬|美国|US|United States|America|洛杉矶|硅谷|🇺🇸)"
private const val OFFICIAL_MRS_GEOSITE_URL =
    "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo/geosite/%s.mrs"
private const val OFFICIAL_MRS_GEOIP_URL =
    "https://raw.githubusercontent.com/MetaCubeX/meta-rules-dat/meta/geo/geoip/%s.mrs"
private const val OFFICIAL_MRS_AUTO_GROUP_NAME = "Auto"
private const val OFFICIAL_MRS_FALLBACK_GROUP_NAME = "Fallback"
private const val OFFICIAL_MRS_ICON_APP_BASE_URL =
    "https://raw.githubusercontent.com/fmz200/wool_scripts/main/icons/apps"
private const val OFFICIAL_MRS_CATALOG_BASE_URL =
    "https://raw.githubusercontent.com/Orz-3/mini/master/Color"

data class OverridePresetTemplateSelection(
    val urlTestRegions: Set<OverridePresetRegion> = emptySet(),
    val fallbackRegions: Set<OverridePresetRegion> = emptySet(),
    val enabledItems: Set<OverridePresetItem> = defaultEnabledPresetItems(),
    val enableUrlTestGroup: Boolean = true,
    val enableFallbackGroup: Boolean = false,
)

data class OverridePresetTemplateContentAnalysis(
    val selection: OverridePresetTemplateSelection,
    val matchesTemplateExactly: Boolean,
)

enum class OverridePresetRegion(
    val specId: String,
    val displayName: String,
    val groupName: String,
    val fallbackGroupName: String,
    val filter: String? = null,
    val excludeFilter: String? = null,
    val icon: String,
) {
    HK(
        specId = "hk",
        displayName = "香港自动组",
        groupName = "HK Auto",
        fallbackGroupName = "HK Fallback",
        filter = "(?i)(香港|HK|Hong Kong|🇭🇰)",
        icon = officialMrsCatalogIconUrl("HK").orEmpty(),
    ),
    TW(
        specId = "tw",
        displayName = "台湾自动组",
        groupName = "TW Auto",
        fallbackGroupName = "TW Fallback",
        filter = "(?i)(台湾|TW|Taiwan|🇹🇼)",
        icon = officialMrsCatalogIconUrl("TW").orEmpty(),
    ),
    JP(
        specId = "jp",
        displayName = "日本自动组",
        groupName = "JP Auto",
        fallbackGroupName = "JP Fallback",
        filter = "(?i)(日本|JP|Japan|东京|大阪|🇯🇵)",
        icon = officialMrsCatalogIconUrl("JP").orEmpty(),
    ),
    SG(
        specId = "sg",
        displayName = "新加坡自动组",
        groupName = "SG Auto",
        fallbackGroupName = "SG Fallback",
        filter = "(?i)(新加坡|SG|Singapore|狮城|🇸🇬)",
        icon = officialMrsCatalogIconUrl("SG").orEmpty(),
    ),
    US(
        specId = "us",
        displayName = "美国自动组",
        groupName = "US Auto",
        fallbackGroupName = "US Fallback",
        filter = "(?i)(美国|US|United States|America|洛杉矶|硅谷|🇺🇸)",
        icon = officialMrsCatalogIconUrl("US").orEmpty(),
    ),
    Other(
        specId = "other",
        displayName = "冷门地区自动组",
        groupName = "Other Auto",
        fallbackGroupName = "Other Fallback",
        excludeFilter = OFFICIAL_MRS_POPULAR_REGION_EXCLUDE_FILTER,
        icon = officialMrsCatalogIconUrl("XD").orEmpty(),
    ),
}

enum class OverridePresetItem(
    val id: String,
    val title: String,
    val summary: String,
    val icon: String? = null,
    val isService: Boolean = false,
    val groupName: String? = null,
    val providers: List<OfficialMrsProviderSpec> = emptyList(),
    val rules: List<OfficialMrsRuleSpec> = emptyList(),
    val detectionRules: List<String> = emptyList(),
) {
    Proxy(
        id = "proxy",
        title = "代理规则集",
        summary = "启用 proxy 规则集并走 Proxy。",
        icon = officialMrsCatalogIconUrl("Static"),
        providers = listOf(
            OfficialMrsProviderSpec("proxy_domain", "proxy", OfficialMrsRuleBehavior.Domain),
        ),
        detectionRules = listOf("RULE-SET,proxy_domain,Proxy"),
    ),
    Ads(
        id = "ads",
        title = "广告拦截",
        summary = "启用 category-ads-all 并走 REJECT。",
        icon = officialMrsCatalogIconUrl("Adblock"),
        providers = listOf(
            OfficialMrsProviderSpec("ads_domain", "category-ads-all", OfficialMrsRuleBehavior.Domain),
        ),
        detectionRules = listOf("RULE-SET,ads_domain,REJECT"),
    ),
    Google(
        id = "google",
        title = "Google",
        summary = "启用 Google 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("Google"),
        groupName = "Google",
        providers = listOf(
            OfficialMrsProviderSpec("google_domain", "google", OfficialMrsRuleBehavior.Domain),
            OfficialMrsProviderSpec("google_ip", "google", OfficialMrsRuleBehavior.IpCidr),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("google_domain", "Google"),
            OfficialMrsRuleSpec("google_ip", "Google", noResolve = true),
        ),
        detectionRules = listOf(
            "RULE-SET,google_domain,Google",
            "RULE-SET,google_ip,Google,no-resolve",
        ),
    ),
    Telegram(
        id = "telegram",
        title = "Telegram",
        summary = "启用 Telegram 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("Telegram"),
        groupName = "Telegram",
        providers = listOf(
            OfficialMrsProviderSpec("telegram_domain", "telegram", OfficialMrsRuleBehavior.Domain),
            OfficialMrsProviderSpec("telegram_ip", "telegram", OfficialMrsRuleBehavior.IpCidr),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("telegram_domain", "Telegram"),
            OfficialMrsRuleSpec("telegram_ip", "Telegram", noResolve = true),
        ),
        detectionRules = listOf(
            "RULE-SET,telegram_domain,Telegram",
            "RULE-SET,telegram_ip,Telegram,no-resolve",
        ),
    ),
    WhatsApp(
        id = "whatsapp",
        title = "WhatsApp",
        summary = "启用 WhatsApp 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("WhatsApp.png"),
        groupName = "WhatsApp",
        providers = listOf(
            OfficialMrsProviderSpec("whatsapp_domain", "whatsapp", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("whatsapp_domain", "WhatsApp"),
        ),
        detectionRules = listOf("RULE-SET,whatsapp_domain,WhatsApp"),
    ),
    Line(
        id = "line",
        title = "LINE",
        summary = "启用 LINE 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("LineTV"),
        groupName = "LINE",
        providers = listOf(
            OfficialMrsProviderSpec("line_domain", "line", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("line_domain", "LINE"),
        ),
        detectionRules = listOf("RULE-SET,line_domain,LINE"),
    ),
    Twitter(
        id = "twitter",
        title = "Twitter / X",
        summary = "启用 Twitter / X 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("Twitter"),
        groupName = "Twitter",
        providers = listOf(
            OfficialMrsProviderSpec("twitter_domain", "twitter", OfficialMrsRuleBehavior.Domain),
            OfficialMrsProviderSpec("twitter_ip", "twitter", OfficialMrsRuleBehavior.IpCidr),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("twitter_domain", "Twitter"),
            OfficialMrsRuleSpec("twitter_ip", "Twitter", noResolve = true),
        ),
        detectionRules = listOf(
            "RULE-SET,twitter_domain,Twitter",
            "RULE-SET,twitter_ip,Twitter,no-resolve",
        ),
    ),
    TikTok(
        id = "tiktok",
        title = "TikTok",
        summary = "启用 TikTok 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("TikTok"),
        groupName = "TikTok",
        providers = listOf(
            OfficialMrsProviderSpec("tiktok_domain", "tiktok", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("tiktok_domain", "TikTok"),
        ),
        detectionRules = listOf("RULE-SET,tiktok_domain,TikTok"),
    ),
    Speedtest(
        id = "speedtest",
        title = "Speedtest",
        summary = "启用 Speedtest 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("Speedtest"),
        groupName = "Speedtest",
        providers = listOf(
            OfficialMrsProviderSpec("speedtest_domain", "speedtest", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("speedtest_domain", "Speedtest"),
        ),
        detectionRules = listOf("RULE-SET,speedtest_domain,Speedtest"),
    ),
    GitHub(
        id = "github",
        title = "GitHub",
        summary = "启用 GitHub 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("github_00.png"),
        groupName = "GitHub",
        providers = listOf(
            OfficialMrsProviderSpec("github_domain", "github", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("github_domain", "GitHub"),
        ),
        detectionRules = listOf("RULE-SET,github_domain,GitHub"),
    ),
    Discord(
        id = "discord",
        title = "Discord",
        summary = "启用 Discord 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("Discord.png"),
        groupName = "Discord",
        providers = listOf(
            OfficialMrsProviderSpec("discord_domain", "discord", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("discord_domain", "Discord"),
        ),
        detectionRules = listOf("RULE-SET,discord_domain,Discord"),
    ),
    Reddit(
        id = "reddit",
        title = "Reddit",
        summary = "启用 Reddit 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("Category_Magazine.png"),
        groupName = "Reddit",
        providers = listOf(
            OfficialMrsProviderSpec("reddit_domain", "reddit", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("reddit_domain", "Reddit"),
        ),
        detectionRules = listOf("RULE-SET,reddit_domain,Reddit"),
    ),
    Facebook(
        id = "facebook",
        title = "Facebook",
        summary = "启用 Facebook 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("facebook.png"),
        groupName = "Facebook",
        providers = listOf(
            OfficialMrsProviderSpec("facebook_domain", "facebook", OfficialMrsRuleBehavior.Domain),
            OfficialMrsProviderSpec("facebook_ip", "facebook", OfficialMrsRuleBehavior.IpCidr),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("facebook_domain", "Facebook"),
            OfficialMrsRuleSpec("facebook_ip", "Facebook", noResolve = true),
        ),
        detectionRules = listOf(
            "RULE-SET,facebook_domain,Facebook",
            "RULE-SET,facebook_ip,Facebook,no-resolve",
        ),
    ),
    Instagram(
        id = "instagram",
        title = "Instagram",
        summary = "启用 Instagram 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("Instagram"),
        groupName = "Instagram",
        providers = listOf(
            OfficialMrsProviderSpec("instagram_domain", "instagram", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("instagram_domain", "Instagram"),
        ),
        detectionRules = listOf("RULE-SET,instagram_domain,Instagram"),
    ),
    Threads(
        id = "threads",
        title = "Threads",
        summary = "启用 Threads 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("Threads.png"),
        groupName = "Threads",
        providers = listOf(
            OfficialMrsProviderSpec("threads_domain", "threads", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("threads_domain", "Threads"),
        ),
        detectionRules = listOf("RULE-SET,threads_domain,Threads"),
    ),
    Microsoft(
        id = "microsoft",
        title = "Microsoft",
        summary = "启用 Microsoft 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("Microsoft"),
        groupName = "Microsoft",
        providers = listOf(
            OfficialMrsProviderSpec("microsoft_domain", "microsoft", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("microsoft_domain", "Microsoft"),
        ),
        detectionRules = listOf("RULE-SET,microsoft_domain,Microsoft"),
    ),
    Bing(
        id = "bing",
        title = "Bing",
        summary = "启用 Bing 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("bing.png"),
        groupName = "Bing",
        providers = listOf(
            OfficialMrsProviderSpec("bing_domain", "bing", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("bing_domain", "Bing"),
        ),
        detectionRules = listOf("RULE-SET,bing_domain,Bing"),
    ),
    Apple(
        id = "apple",
        title = "Apple",
        summary = "启用 Apple 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("Apple"),
        groupName = "Apple",
        providers = listOf(
            OfficialMrsProviderSpec("apple_domain", "apple", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("apple_domain", "Apple"),
        ),
        detectionRules = listOf("RULE-SET,apple_domain,Apple"),
    ),
    YouTube(
        id = "youtube",
        title = "YouTube",
        summary = "启用 YouTube 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("YouTube"),
        groupName = "YouTube",
        providers = listOf(
            OfficialMrsProviderSpec("youtube_domain", "youtube", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("youtube_domain", "YouTube"),
        ),
        detectionRules = listOf("RULE-SET,youtube_domain,YouTube"),
    ),
    Netflix(
        id = "netflix",
        title = "Netflix",
        summary = "启用 Netflix 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("Netflix"),
        groupName = "Netflix",
        providers = listOf(
            OfficialMrsProviderSpec("netflix_domain", "netflix", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("netflix_domain", "Netflix"),
        ),
        detectionRules = listOf("RULE-SET,netflix_domain,Netflix"),
    ),
    Disney(
        id = "disney",
        title = "Disney",
        summary = "启用 Disney 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("DisneyPlus"),
        groupName = "Disney",
        providers = listOf(
            OfficialMrsProviderSpec("disney_domain", "disney", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("disney_domain", "Disney"),
        ),
        detectionRules = listOf("RULE-SET,disney_domain,Disney"),
    ),
    Hbo(
        id = "hbo",
        title = "HBO",
        summary = "启用 HBO 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("HBO"),
        groupName = "HBO",
        providers = listOf(
            OfficialMrsProviderSpec("hbo_domain", "hbo", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("hbo_domain", "HBO"),
        ),
        detectionRules = listOf("RULE-SET,hbo_domain,HBO"),
    ),
    PrimeVideo(
        id = "primevideo",
        title = "Prime Video",
        summary = "启用 Prime Video 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("PrimeVideo"),
        groupName = "PrimeVideo",
        providers = listOf(
            OfficialMrsProviderSpec("primevideo_domain", "primevideo", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("primevideo_domain", "PrimeVideo"),
        ),
        detectionRules = listOf("RULE-SET,primevideo_domain,PrimeVideo"),
    ),
    Tvb(
        id = "tvb",
        title = "TVB",
        summary = "启用 TVB 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("TVBAnywhere+.png"),
        groupName = "TVB",
        providers = listOf(
            OfficialMrsProviderSpec("tvb_domain", "tvb", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("tvb_domain", "TVB"),
        ),
        detectionRules = listOf("RULE-SET,tvb_domain,TVB"),
    ),
    MyTvSuper(
        id = "mytvsuper",
        title = "MyTVSuper",
        summary = "启用 MyTVSuper 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("TVBAnywhere+.png"),
        groupName = "MyTVSuper",
        providers = listOf(
            OfficialMrsProviderSpec("mytvsuper_domain", "mytvsuper", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("mytvsuper_domain", "MyTVSuper"),
        ),
        detectionRules = listOf("RULE-SET,mytvsuper_domain,MyTVSuper"),
    ),
    Dazn(
        id = "dazn",
        title = "DAZN",
        summary = "启用 DAZN 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("Streaming"),
        groupName = "DAZN",
        providers = listOf(
            OfficialMrsProviderSpec("dazn_domain", "dazn", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("dazn_domain", "DAZN"),
        ),
        detectionRules = listOf("RULE-SET,dazn_domain,DAZN"),
    ),
    Spotify(
        id = "spotify",
        title = "Spotify",
        summary = "启用 Spotify 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("Spotify"),
        groupName = "Spotify",
        providers = listOf(
            OfficialMrsProviderSpec("spotify_domain", "spotify", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("spotify_domain", "Spotify"),
        ),
        detectionRules = listOf("RULE-SET,spotify_domain,Spotify"),
    ),
    Amazon(
        id = "amazon",
        title = "Amazon",
        summary = "启用 Amazon 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("Amazon.png"),
        groupName = "Amazon",
        providers = listOf(
            OfficialMrsProviderSpec("amazon_domain", "amazon", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("amazon_domain", "Amazon"),
        ),
        detectionRules = listOf("RULE-SET,amazon_domain,Amazon"),
    ),
    PayPal(
        id = "paypal",
        title = "PayPal",
        summary = "启用 PayPal 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("Paypal"),
        groupName = "PayPal",
        providers = listOf(
            OfficialMrsProviderSpec("paypal_domain", "paypal", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("paypal_domain", "PayPal"),
        ),
        detectionRules = listOf("RULE-SET,paypal_domain,PayPal"),
    ),
    Cloudflare(
        id = "cloudflare",
        title = "Cloudflare",
        summary = "启用 Cloudflare 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("Cloudflare.png"),
        groupName = "Cloudflare",
        providers = listOf(
            OfficialMrsProviderSpec("cloudflare_domain", "cloudflare", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("cloudflare_domain", "Cloudflare"),
        ),
        detectionRules = listOf("RULE-SET,cloudflare_domain,Cloudflare"),
    ),
    Zoom(
        id = "zoom",
        title = "Zoom",
        summary = "启用 Zoom 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("Category_Networking.png"),
        groupName = "Zoom",
        providers = listOf(
            OfficialMrsProviderSpec("zoom_domain", "zoom", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("zoom_domain", "Zoom"),
        ),
        detectionRules = listOf("RULE-SET,zoom_domain,Zoom"),
    ),
    Wikimedia(
        id = "wikimedia",
        title = "Wikimedia",
        summary = "启用 Wikimedia 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("Category_Research.png"),
        groupName = "Wikimedia",
        providers = listOf(
            OfficialMrsProviderSpec("wikimedia_domain", "wikimedia", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("wikimedia_domain", "Wikimedia"),
        ),
        detectionRules = listOf("RULE-SET,wikimedia_domain,Wikimedia"),
    ),
    Bilibili(
        id = "bilibili",
        title = "Bilibili",
        summary = "启用 Bilibili 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("Bili"),
        groupName = "Bilibili",
        providers = listOf(
            OfficialMrsProviderSpec("bilibili_domain", "bilibili", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("bilibili_domain", "Bilibili"),
        ),
        detectionRules = listOf("RULE-SET,bilibili_domain,Bilibili"),
    ),
    BiliIntl(
        id = "biliintl",
        title = "BiliIntl",
        summary = "启用 BiliIntl 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("Bili"),
        groupName = "BiliIntl",
        providers = listOf(
            OfficialMrsProviderSpec("biliintl_domain", "biliintl", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("biliintl_domain", "BiliIntl"),
        ),
        detectionRules = listOf("RULE-SET,biliintl_domain,BiliIntl"),
    ),
    Bahamut(
        id = "bahamut",
        title = "Bahamut",
        summary = "启用 Bahamut 分流和专属策略组。",
        isService = true,
        icon = officialMrsCatalogIconUrl("Bahamut"),
        groupName = "Bahamut",
        providers = listOf(
            OfficialMrsProviderSpec("bahamut_domain", "bahamut", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("bahamut_domain", "Bahamut"),
        ),
        detectionRules = listOf("RULE-SET,bahamut_domain,Bahamut"),
    ),
    Dmm(
        id = "dmm",
        title = "DMM",
        summary = "启用 DMM 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("AnimeHome.png"),
        groupName = "DMM",
        providers = listOf(
            OfficialMrsProviderSpec("dmm_domain", "dmm", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("dmm_domain", "DMM"),
        ),
        detectionRules = listOf("RULE-SET,dmm_domain,DMM"),
    ),
    Abema(
        id = "abema",
        title = "Abema",
        summary = "启用 Abema 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("AnimeHome.png"),
        groupName = "Abema",
        providers = listOf(
            OfficialMrsProviderSpec("abema_domain", "abema", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("abema_domain", "Abema"),
        ),
        detectionRules = listOf("RULE-SET,abema_domain,Abema"),
    ),
    Ehentai(
        id = "ehentai",
        title = "EHentai",
        summary = "启用 EHentai 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("HentaiHome.png"),
        groupName = "EHentai",
        providers = listOf(
            OfficialMrsProviderSpec("ehentai_domain", "ehentai", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("ehentai_domain", "EHentai"),
        ),
        detectionRules = listOf("RULE-SET,ehentai_domain,EHentai"),
    ),
    OpenAI(
        id = "openai",
        title = "OpenAI",
        summary = "启用 OpenAI 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("ChatGPT.png"),
        groupName = "OpenAI",
        providers = listOf(
            OfficialMrsProviderSpec("openai_domain", "openai", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("openai_domain", "OpenAI"),
        ),
        detectionRules = listOf("RULE-SET,openai_domain,OpenAI"),
    ),
    Anthropic(
        id = "anthropic",
        title = "Claude / Anthropic",
        summary = "启用 Claude / Anthropic 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("Claude_01.png"),
        groupName = "Claude",
        providers = listOf(
            OfficialMrsProviderSpec("anthropic_domain", "anthropic", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("anthropic_domain", "Claude"),
        ),
        detectionRules = listOf("RULE-SET,anthropic_domain,Claude"),
    ),
    OneDrive(
        id = "onedrive",
        title = "OneDrive",
        summary = "启用 OneDrive 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("OneDrive.png"),
        groupName = "OneDrive",
        providers = listOf(
            OfficialMrsProviderSpec("onedrive_domain", "onedrive", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("onedrive_domain", "OneDrive"),
        ),
        detectionRules = listOf("RULE-SET,onedrive_domain,OneDrive"),
    ),
    Pixiv(
        id = "pixiv",
        title = "Pixiv",
        summary = "启用 Pixiv 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("Category_Photo.png"),
        groupName = "Pixiv",
        providers = listOf(
            OfficialMrsProviderSpec("pixiv_domain", "pixiv", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("pixiv_domain", "Pixiv"),
        ),
        detectionRules = listOf("RULE-SET,pixiv_domain,Pixiv"),
    ),
    Niconico(
        id = "niconico",
        title = "Niconico",
        summary = "启用 Niconico 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("AnimeHome.png"),
        groupName = "Niconico",
        providers = listOf(
            OfficialMrsProviderSpec("niconico_domain", "niconico", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("niconico_domain", "Niconico"),
        ),
        detectionRules = listOf("RULE-SET,niconico_domain,Niconico"),
    ),
    Steam(
        id = "steam",
        title = "Steam",
        summary = "启用 Steam 分流和专属策略组。",
        isService = true,
        icon = officialMrsAppIconUrl("steam.png"),
        groupName = "Steam",
        providers = listOf(
            OfficialMrsProviderSpec("steam_domain", "steam", OfficialMrsRuleBehavior.Domain),
        ),
        rules = listOf(
            OfficialMrsRuleSpec("steam_domain", "Steam"),
        ),
        detectionRules = listOf("RULE-SET,steam_domain,Steam"),
    ),
    Cn(
        id = "cn",
        title = "中国大陆直连",
        summary = "启用 cn 域名和 IP 规则并走 DIRECT。",
        icon = officialMrsCatalogIconUrl("China"),
        providers = listOf(
            OfficialMrsProviderSpec("cn_domain", "cn", OfficialMrsRuleBehavior.Domain),
            OfficialMrsProviderSpec("cn_ip", "cn", OfficialMrsRuleBehavior.IpCidr),
        ),
        detectionRules = listOf(
            "RULE-SET,cn_domain,DIRECT",
            "RULE-SET,cn_ip,DIRECT,no-resolve",
        ),
    ),
    GeolocationNotCn(
        id = "geolocation_not_cn",
        title = "境外地理规则",
        summary = "启用 geolocation-!cn 并走 Proxy。",
        icon = officialMrsCatalogIconUrl("Global"),
        providers = listOf(
            OfficialMrsProviderSpec(
                "geolocation_not_cn_domain",
                "geolocation-!cn",
                OfficialMrsRuleBehavior.Domain,
            ),
        ),
        detectionRules = listOf("RULE-SET,geolocation_not_cn_domain,Proxy"),
    ),
    Match(
        id = "match",
        title = "兜底 MATCH",
        summary = "末尾追加 MATCH,Proxy。",
        icon = officialMrsCatalogIconUrl("Final"),
        detectionRules = listOf("MATCH,Proxy"),
    ),
}

private enum class OfficialMrsHealthCheckGroupType(
    val wireName: String,
) {
    UrlTest("url-test"),
    Fallback("fallback"),
}

enum class OfficialMrsRuleBehavior(
    val wireName: String,
) {
    Domain("domain"),
    IpCidr("ipcidr"),
}

data class OfficialMrsProviderSpec(
    val id: String,
    val remoteName: String,
    val behavior: OfficialMrsRuleBehavior,
)

data class OfficialMrsRuleSpec(
    val providerId: String,
    val target: String,
    val noResolve: Boolean = false,
)

private val orderedRegions = OverridePresetRegion.entries.toList()
private val orderedItems = OverridePresetItem.entries.toList()
private val orderedServiceItems = orderedItems.filter(OverridePresetItem::isService)
private val orderedBaseItems = orderedItems.filterNot(OverridePresetItem::isService)
private val itemById = orderedItems.associateBy(OverridePresetItem::id)
private val regionById = orderedRegions.associateBy(OverridePresetRegion::specId)
private val templateProviderIds = orderedItems
    .flatMap { it.providers }
    .map(OfficialMrsProviderSpec::id)
    .toSet()
private val serviceGroupNames = orderedServiceItems
    .mapNotNull(OverridePresetItem::groupName)
    .toSet()
private val regionGroupNames = orderedRegions
    .flatMap { region -> listOf(region.groupName, region.fallbackGroupName) }
    .toSet()
private val defaultEnabledItemIds = linkedSetOf(
    "proxy",
    "ads",
    "google",
    "telegram",
    "github",
    "microsoft",
    "bing",
    "apple",
    "youtube",
    "netflix",
    "spotify",
    "openai",
    "anthropic",
    "steam",
    "cn",
    "geolocation_not_cn",
    "match",
)
private val ruleOrder = listOf(
    "ads",
    "google",
    "telegram",
    "whatsapp",
    "line",
    "twitter",
    "tiktok",
    "speedtest",
    "github",
    "discord",
    "reddit",
    "facebook",
    "instagram",
    "threads",
    "microsoft",
    "bing",
    "apple",
    "youtube",
    "netflix",
    "disney",
    "hbo",
    "primevideo",
    "tvb",
    "mytvsuper",
    "dazn",
    "spotify",
    "amazon",
    "paypal",
    "cloudflare",
    "zoom",
    "wikimedia",
    "bilibili",
    "biliintl",
    "bahamut",
    "dmm",
    "abema",
    "ehentai",
    "openai",
    "anthropic",
    "onedrive",
    "pixiv",
    "niconico",
    "steam",
    "cn",
    "proxy",
    "geolocation_not_cn",
    "match",
)
private val templateRules = orderedItems
    .flatMap(OverridePresetItem::detectionRules)
    .toSet()

fun defaultEnabledPresetItems(): Set<OverridePresetItem> {
    return defaultEnabledItemIds
        .mapNotNull(itemById::get)
        .toCollection(linkedSetOf())
}

fun orderedPresetRegions(): List<OverridePresetRegion> = orderedRegions

fun orderedBasePresetItems(): List<OverridePresetItem> = orderedBaseItems

fun orderedServicePresetItems(): List<OverridePresetItem> = orderedServiceItems

fun presetGroupTypeIconUrl(type: String): String? {
    return when (type) {
        "urltest" -> officialMrsCatalogIconUrl("Urltest")
        "fallback" -> officialMrsCatalogIconUrl("Available")
        else -> null
    }
}

fun sortPresetRegions(regions: Collection<OverridePresetRegion>): List<OverridePresetRegion> {
    val selectedIds = regions.map(OverridePresetRegion::specId).toSet()
    return orderedRegions.filter { it.specId in selectedIds }
}

fun sortPresetItems(items: Collection<OverridePresetItem>): List<OverridePresetItem> {
    val selectedIds = items.map(OverridePresetItem::id).toSet()
    return orderedItems.filter { it.id in selectedIds }
}

fun defaultOverridePresetTemplateSelection(): OverridePresetTemplateSelection {
    return OverridePresetTemplateSelection(
        enabledItems = defaultEnabledPresetItems(),
        enableUrlTestGroup = true,
        enableFallbackGroup = false,
    )
}

fun analyzePresetTemplateContent(content: String?): OverridePresetTemplateContentAnalysis {
    if (content.isNullOrBlank()) {
        return OverridePresetTemplateContentAnalysis(
            selection = defaultOverridePresetTemplateSelection(),
            matchesTemplateExactly = true,
        )
    }

    val document = runCatching { YamlCodec.loadValue(content).asStringKeyedMap() }.getOrElse {
        return OverridePresetTemplateContentAnalysis(
            selection = defaultOverridePresetTemplateSelection(),
            matchesTemplateExactly = false,
        )
    } ?: return OverridePresetTemplateContentAnalysis(
        selection = defaultOverridePresetTemplateSelection(),
        matchesTemplateExactly = false,
    )

    val selection = inferPresetTemplateSelection(document) ?: return OverridePresetTemplateContentAnalysis(
        selection = defaultOverridePresetTemplateSelection(),
        matchesTemplateExactly = false,
    )

    val generatedDocument = runCatching {
        buildPresetTemplateYaml(selection)
            .let(YamlCodec::loadValue)
            .asStringKeyedMap()
    }.getOrNull()

    return OverridePresetTemplateContentAnalysis(
        selection = selection,
        matchesTemplateExactly = generatedDocument != null && document == generatedDocument,
    )
}

fun inferPresetTemplateSelection(content: String?): OverridePresetTemplateSelection {
    return analyzePresetTemplateContent(content).selection
}

private fun inferPresetTemplateSelection(document: Map<String, Any?>): OverridePresetTemplateSelection? {
    val providerKeys = document.stringKeyedMap("rule-providers").keys
    val groupNames = document.listOfMaps("proxy-groups")
        .mapNotNull { group -> group["name"]?.toString()?.takeIf(String::isNotBlank) }
        .toSet()
    val rules = document.stringList("rules")

    val hasTemplateSignals = providerKeys.any(templateProviderIds::contains) ||
        groupNames.any(serviceGroupNames::contains) ||
        groupNames.any(regionGroupNames::contains) ||
        rules.any(::isOfficialMrsTemplateRule)

    if (!hasTemplateSignals) {
        return null
    }

    val inferredUrlTestRegions = orderedRegions
        .filter { it.groupName in groupNames }
        .toCollection(linkedSetOf())
    val inferredFallbackRegions = orderedRegions
        .filter { it.fallbackGroupName in groupNames }
        .toCollection(linkedSetOf())
    val inferredEnabledItems = orderedItems
        .filter { item ->
            isOfficialMrsItemEnabledInConfig(
                item = item,
                providerKeys = providerKeys,
                groupNames = groupNames,
                rules = rules,
            )
        }
        .toCollection(linkedSetOf())
        .ifEmpty { defaultEnabledPresetItems() }

    return OverridePresetTemplateSelection(
        urlTestRegions = inferredUrlTestRegions,
        fallbackRegions = inferredFallbackRegions,
        enabledItems = inferredEnabledItems,
        enableUrlTestGroup = OFFICIAL_MRS_AUTO_GROUP_NAME in groupNames ||
            orderedRegions.any { it.groupName in groupNames },
        enableFallbackGroup = OFFICIAL_MRS_FALLBACK_GROUP_NAME in groupNames ||
            orderedRegions.any { it.fallbackGroupName in groupNames },
    )
}

fun buildPresetTemplateYaml(selection: OverridePresetTemplateSelection): String {
    val normalizedEnabledItems = normalizeEnabledItems(selection.enabledItems)
    val selectedUrlTestRegions = orderedRegions.filter { it in selection.urlTestRegions }
    val selectedFallbackRegions = orderedRegions.filter { it in selection.fallbackRegions }
    val document = linkedMapOf<String, Any?>(
        "rule-providers" to buildRuleProviders(normalizedEnabledItems),
        "proxy-groups" to buildProxyGroups(
            selectedUrlTestRegions = selectedUrlTestRegions,
            selectedFallbackRegions = selectedFallbackRegions,
            enabledItems = normalizedEnabledItems,
            enableUrlTestGroup = selection.enableUrlTestGroup,
            enableFallbackGroup = selection.enableFallbackGroup,
        ),
        "rules" to buildRules(normalizedEnabledItems),
    ).filterValues { value ->
        when (value) {
            is Collection<*> -> value.isNotEmpty()
            is Map<*, *> -> value.isNotEmpty()
            else -> value != null
        }
    }

    val yamlContent = YamlCodec.dumpMap(document)
    runCatching {
        YamlCodec.validate(yamlContent)
        YamlCodec.loadMap(yamlContent)
    }.getOrElse { error ->
        throw IllegalStateException(
            "Custom routing YAML self-check failed: ${error.message}",
            error,
        )
    }
    return yamlContent
}

private fun normalizeEnabledItems(items: Set<OverridePresetItem>): Set<OverridePresetItem> {
    return items.ifEmpty { linkedSetOf(OverridePresetItem.Match) }
}

private fun buildRuleProviders(
    enabledItems: Set<OverridePresetItem>,
): Map<String, Map<String, Any?>> {
    return linkedMapOf<String, Map<String, Any?>>().apply {
        orderedItems
            .filter { item -> item in enabledItems && item != OverridePresetItem.Match }
            .flatMap(OverridePresetItem::providers)
            .forEach { provider ->
                put(
                    provider.id,
                    linkedMapOf(
                        "type" to "http",
                        "format" to "mrs",
                        "behavior" to provider.behavior.wireName,
                        "url" to provider.urlTemplate().format(provider.remoteName),
                        "path" to "./providers/rules/${provider.id}.mrs",
                        "interval" to OFFICIAL_MRS_RULE_PROVIDER_INTERVAL,
                    ),
                )
            }
    }
}

private fun buildProxyGroups(
    selectedUrlTestRegions: List<OverridePresetRegion>,
    selectedFallbackRegions: List<OverridePresetRegion>,
    enabledItems: Set<OverridePresetItem>,
    enableUrlTestGroup: Boolean,
    enableFallbackGroup: Boolean,
): List<Map<String, Any?>> {
    val regionNames = buildList {
        if (enableUrlTestGroup) {
            addAll(selectedUrlTestRegions.map(OverridePresetRegion::groupName))
        }
        if (enableFallbackGroup) {
            addAll(selectedFallbackRegions.map(OverridePresetRegion::fallbackGroupName))
        }
    }

    return buildList {
        add(
            buildProxySelectGroup(
                regionNames = regionNames,
                enableUrlTestGroup = enableUrlTestGroup,
                enableFallbackGroup = enableFallbackGroup,
            ),
        )
        if (enableUrlTestGroup) {
            add(buildHealthCheckGroup(name = OFFICIAL_MRS_AUTO_GROUP_NAME, type = OfficialMrsHealthCheckGroupType.UrlTest))
        }
        if (enableFallbackGroup) {
            add(buildHealthCheckGroup(name = OFFICIAL_MRS_FALLBACK_GROUP_NAME, type = OfficialMrsHealthCheckGroupType.Fallback))
        }
        addAll(
            orderedServiceItems
                .filter { it in enabledItems }
                .map { item ->
                    buildServiceSelectGroup(
                        item = item,
                        regionNames = regionNames,
                        enableUrlTestGroup = enableUrlTestGroup,
                        enableFallbackGroup = enableFallbackGroup,
                    )
                },
        )
        if (enableUrlTestGroup) {
            addAll(
                selectedUrlTestRegions.map { region ->
                    buildHealthCheckGroup(
                        name = region.groupName,
                        type = OfficialMrsHealthCheckGroupType.UrlTest,
                        filter = region.filter,
                        excludeFilter = region.excludeFilter,
                        icon = region.icon,
                    )
                },
            )
        }
        if (enableFallbackGroup) {
            addAll(
                selectedFallbackRegions.map { region ->
                    buildHealthCheckGroup(
                        name = region.fallbackGroupName,
                        type = OfficialMrsHealthCheckGroupType.Fallback,
                        filter = region.filter,
                        excludeFilter = region.excludeFilter,
                        icon = region.icon,
                    )
                },
            )
        }
    }
}

private fun buildRules(enabledItems: Set<OverridePresetItem>): List<String> {
    val enabledItemIds = enabledItems.map(OverridePresetItem::id).toSet()
    return buildList {
        ruleOrder
            .mapNotNull(itemById::get)
            .filter { it.id in enabledItemIds }
            .forEach { item -> addAll(item.detectionRules) }
        if ("match" in enabledItemIds || isEmpty()) {
            if ("MATCH,Proxy" !in this) {
                add("MATCH,Proxy")
            }
        }
    }
}

private fun buildHealthCheckGroup(
    name: String,
    type: OfficialMrsHealthCheckGroupType,
    filter: String? = null,
    excludeFilter: String? = null,
    icon: String = when (type) {
        OfficialMrsHealthCheckGroupType.UrlTest -> officialMrsCatalogIconUrl("Urltest").orEmpty()
        OfficialMrsHealthCheckGroupType.Fallback -> officialMrsCatalogIconUrl("Available").orEmpty()
    },
): Map<String, Any?> {
    return linkedMapOf<String, Any?>().apply {
        put("name", name)
        put("type", type.wireName)
        put("icon", icon)
        put("url", OFFICIAL_MRS_URL_TEST_URL)
        put("interval", OFFICIAL_MRS_URL_TEST_INTERVAL)
        put("include-all", true)
        put("exclude-filter", combineOfficialMrsExcludeFilters(excludeFilter))
        filter?.let { put("filter", it) }
    }
}

private fun buildProxySelectGroup(
    regionNames: List<String>,
    enableUrlTestGroup: Boolean,
    enableFallbackGroup: Boolean,
): Map<String, Any?> {
    return linkedMapOf(
        "name" to "Proxy",
        "type" to "select",
        "icon" to OverridePresetItem.Proxy.icon.orEmpty(),
        "proxies" to buildSelectableGroupNames(
            regionNames = regionNames,
            enableUrlTestGroup = enableUrlTestGroup,
            enableFallbackGroup = enableFallbackGroup,
        ),
        "include-all" to true,
    )
}

private fun buildServiceSelectGroup(
    item: OverridePresetItem,
    regionNames: List<String>,
    enableUrlTestGroup: Boolean,
    enableFallbackGroup: Boolean,
): Map<String, Any?> {
    val serviceGroupName = checkNotNull(item.groupName)
    return linkedMapOf<String, Any?>().apply {
        put("name", serviceGroupName)
        put("type", "select")
        item.icon?.takeIf(String::isNotBlank)?.let { put("icon", it) }
        put(
            "proxies",
            buildList {
                add("Proxy")
                add("DIRECT")
                if (enableUrlTestGroup) {
                    add(OFFICIAL_MRS_AUTO_GROUP_NAME)
                }
                if (enableFallbackGroup) {
                    add(OFFICIAL_MRS_FALLBACK_GROUP_NAME)
                }
                addAll(regionNames)
            }.distinct(),
        )
    }
}

private fun buildSelectableGroupNames(
    regionNames: List<String>,
    enableUrlTestGroup: Boolean,
    enableFallbackGroup: Boolean,
): List<String> {
    return buildList {
        if (enableUrlTestGroup) {
            add(OFFICIAL_MRS_AUTO_GROUP_NAME)
        }
        if (enableFallbackGroup) {
            add(OFFICIAL_MRS_FALLBACK_GROUP_NAME)
        }
        addAll(regionNames)
    }.distinct()
}

private fun combineOfficialMrsExcludeFilters(extraExcludeFilter: String?): String {
    return listOf(OFFICIAL_MRS_EXCLUDE_FILTER, extraExcludeFilter)
        .filterNotNull()
        .joinToString("|")
}

private fun OfficialMrsProviderSpec.urlTemplate(): String {
    return when (behavior) {
        OfficialMrsRuleBehavior.Domain -> OFFICIAL_MRS_GEOSITE_URL
        OfficialMrsRuleBehavior.IpCidr -> OFFICIAL_MRS_GEOIP_URL
    }
}

private fun isOfficialMrsItemEnabledInConfig(
    item: OverridePresetItem,
    providerKeys: Set<String>,
    groupNames: Set<String>,
    rules: List<String>,
): Boolean {
    val providerIds = item.providers.map(OfficialMrsProviderSpec::id)
    return when (item.id) {
        "match" -> rules.any { rule -> rule.trim() == "MATCH,Proxy" }
        else -> providerIds.any(providerKeys::contains) ||
            item.detectionRules.any(rules::contains) ||
            (item.groupName != null && item.groupName in groupNames)
    }
}

private fun isOfficialMrsTemplateRule(rule: String): Boolean {
    val normalizedRule = rule.trim()
    return normalizedRule in templateRules ||
        templateProviderIds.any { providerId -> normalizedRule.contains(providerId) }
}

private fun Map<String, Any?>.stringKeyedMap(key: String): Map<String, Any?> {
    return (this[key] as? Map<*, *>)?.entries
        ?.associate { entry -> entry.key.toString() to entry.value }
        .orEmpty()
}

private fun Map<String, Any?>.listOfMaps(key: String): List<Map<String, Any?>> {
    return (this[key] as? List<*>)?.mapNotNull { element ->
        (element as? Map<*, *>)?.entries?.associate { entry -> entry.key.toString() to entry.value }
    }.orEmpty()
}

private fun Map<String, Any?>.stringList(key: String): List<String> {
    return (this[key] as? List<*>)?.mapNotNull { value ->
        when (value) {
            null -> null
            is String -> value
            else -> value.toString()
        }
    }.orEmpty()
}

private fun Any?.asStringKeyedMap(): Map<String, Any?>? {
    return (this as? Map<*, *>)?.entries
        ?.associate { entry -> entry.key.toString() to entry.value }
}

private fun officialMrsCatalogIconUrl(iconName: String?): String? {
    val normalizedIconName = iconName?.trim()?.takeIf(String::isNotBlank) ?: return null
    return "$OFFICIAL_MRS_CATALOG_BASE_URL/${encodePathSegment(normalizedIconName)}.png"
}

private fun officialMrsAppIconUrl(iconName: String?): String? {
    val normalizedIconName = iconName?.trim()?.takeIf(String::isNotBlank) ?: return null
    return "$OFFICIAL_MRS_ICON_APP_BASE_URL/${encodePathSegment(normalizedIconName)}"
}

private fun encodePathSegment(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
        .replace("+", "%20")
}
