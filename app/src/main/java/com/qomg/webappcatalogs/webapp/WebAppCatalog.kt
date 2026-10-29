package com.qomg.webappcatalogs.webapp

data class CatalogEntry(val name: String, val url: String, val category: String)

/** Built-in list of popular web apps, shown on the catalog tab. */
object WebAppCatalog {
    val entries = listOf(
        CatalogEntry("ChatGPT", "https://chatgpt.com", "AI"),
        CatalogEntry("Claude", "https://claude.ai", "AI"),
        CatalogEntry("Gemini", "https://gemini.google.com", "AI"),
        CatalogEntry("DeepSeek", "https://chat.deepseek.com", "AI"),
        CatalogEntry("Gmail", "https://mail.google.com", "效率"),
        CatalogEntry("Google Calendar", "https://calendar.google.com", "效率"),
        CatalogEntry("Notion", "https://www.notion.so", "效率"),
        CatalogEntry("Trello", "https://trello.com", "效率"),
        CatalogEntry("语雀", "https://www.yuque.com", "效率"),
        CatalogEntry("飞书", "https://www.feishu.cn/messenger", "沟通"),
        CatalogEntry("Slack", "https://app.slack.com", "沟通"),
        CatalogEntry("Discord", "https://discord.com/app", "沟通"),
        CatalogEntry("Telegram", "https://web.telegram.org", "沟通"),
        CatalogEntry("微博", "https://m.weibo.cn", "社交媒体"),
        CatalogEntry("小红书", "https://www.xiaohongshu.com", "社交媒体"),
        CatalogEntry("抖音", "https://www.douyin.com", "社交媒体"),
        CatalogEntry("豆瓣", "https://m.douban.com", "社交媒体"),
        CatalogEntry("X", "https://x.com", "社交媒体"),
        CatalogEntry("Facebook", "https://m.facebook.com", "社交媒体"),
        CatalogEntry("Instagram", "https://www.instagram.com", "社交媒体"),
        CatalogEntry("Threads", "https://www.threads.com", "社交媒体"),
        CatalogEntry("TikTok", "https://www.tiktok.com", "社交媒体"),
        CatalogEntry("LinkedIn", "https://www.linkedin.com", "社交媒体"),
        CatalogEntry("Reddit", "https://www.reddit.com", "社交媒体"),
        CatalogEntry("Pinterest", "https://www.pinterest.com", "社交媒体"),
        CatalogEntry("Bluesky", "https://bsky.app", "社交媒体"),
        CatalogEntry("Mastodon", "https://mastodon.social", "社交媒体"),
        CatalogEntry("GitHub", "https://github.com", "开发"),
        CatalogEntry("GitLab", "https://gitlab.com", "开发"),
        CatalogEntry("Stack Overflow", "https://stackoverflow.com", "开发"),
        CatalogEntry("YouTube", "https://m.youtube.com", "娱乐资讯"),
        CatalogEntry("哔哩哔哩", "https://m.bilibili.com", "娱乐资讯"),
        CatalogEntry("知乎", "https://www.zhihu.com", "娱乐资讯"),
    )
}
