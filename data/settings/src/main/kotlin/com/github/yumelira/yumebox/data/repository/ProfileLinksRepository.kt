package com.github.yumelira.yumebox.data.repository

import com.github.yumelira.yumebox.data.store.LinkOpenMode
import com.github.yumelira.yumebox.data.store.Preference
import com.github.yumelira.yumebox.data.store.ProfileLink
import com.github.yumelira.yumebox.data.store.ProfileLinksStorage

class ProfileLinksRepository(
    private val storage: ProfileLinksStorage,
) {
    val linkOpenMode: Preference<LinkOpenMode> = storage.linkOpenMode
    val links: Preference<List<ProfileLink>> = storage.links
    val defaultLinkId: Preference<String> = storage.defaultLinkId
}
