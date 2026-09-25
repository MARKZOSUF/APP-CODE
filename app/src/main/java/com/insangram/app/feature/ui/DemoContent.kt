package com.insangram.app.feature.ui

/**
 * Presentation-only sample content used to render the visual design before the
 * repositories are wired into ViewModels. Every field mirrors a real domain
 * model field name so swapping the source later is a mechanical change.
 */
data class DemoUser(
    val username: String,
    val name: String,
    val avatar: String,
    val bio: String = "",
    val seenStory: Boolean = false,
)

data class DemoPost(
    val id: String,
    val author: DemoUser,
    val location: String,
    val image: String,
    val pageCount: Int,
    val likes: String,
    val caption: String,
    val hashtags: String,
    val liked: Boolean = false,
)

data class DemoReel(
    val id: String,
    val author: DemoUser,
    val image: String,
    val caption: String,
    val audio: String,
    val likes: String,
    val comments: String,
    val shares: String,
)

data class DemoTile(val image: String, val tag: String)

data class DemoChat(
    val user: DemoUser,
    val preview: String,
    val time: String,
    val unread: Boolean = false,
)

data class DemoMessage(val text: String, val time: String, val mine: Boolean, val image: String? = null)

data class DemoNotification(
    val user: DemoUser,
    val text: String,
    val time: String,
    val follow: Boolean = false,
    val thumbnail: String? = null,
)

data class DemoHighlight(val label: String, val image: String)

/** Deterministic remote placeholders so every build shows the same layout. */
private fun photo(seed: String, w: Int = 800, h: Int = 800) =
    "https://picsum.photos/seed/$seed/$w/$h"

private fun avatar(index: Int) = "https://i.pravatar.cc/200?img=$index"

object DemoContent {

    val viewer = DemoUser(
        username = "monu_07",
        name = "Monu Kumar",
        avatar = avatar(12),
        bio = "Dreamer | Coder | Future Builder",
    )

    val storyUsers = listOf(
        DemoUser("riya_07", "Riya", avatar(5)),
        DemoUser("shivam_21", "Shivam", avatar(13)),
        DemoUser("anushka", "Anushka", avatar(9)),
        DemoUser("kartik", "Kartik", avatar(14)),
        DemoUser("priyanshu_03", "Priyanshu", avatar(15), seenStory = true),
        DemoUser("neha_singh", "Neha", avatar(20), seenStory = true),
    )

    val posts = listOf(
        DemoPost(
            id = "post-1",
            author = DemoUser("travel.diaries", "Travel Diaries", avatar(33)),
            location = "Manali, Himachal Pradesh",
            image = photo("mountain-trek", 1080, 1080),
            pageCount = 5,
            likes = "12,458",
            caption = "Nature hits different \uD83D\uDC99",
            hashtags = "#mountains #travel #explore",
            liked = true,
        ),
        DemoPost(
            id = "post-2",
            author = DemoUser("nature.vibes", "Nature Vibes", avatar(48)),
            location = "Goa, India",
            image = photo("golden-sunset", 1080, 1080),
            pageCount = 1,
            likes = "8,204",
            caption = "Sunsets are proof that endings can be beautiful",
            hashtags = "#sunset #beach #golden",
        ),
        DemoPost(
            id = "post-3",
            author = DemoUser("code.daily", "Code Daily", avatar(60)),
            location = "Bengaluru",
            image = photo("desk-setup", 1080, 1080),
            pageCount = 3,
            likes = "3,912",
            caption = "Late night builds are the best builds",
            hashtags = "#developer #android #kotlin",
        ),
    )

    val reels = listOf(
        DemoReel(
            id = "reel-1",
            author = DemoUser("nature.vibes", "Nature Vibes", avatar(48)),
            image = photo("ocean-sunset", 1080, 1920),
            caption = "Sunsets are proof that endings can be beautiful... \u2728",
            audio = "Original audio - nature.vibes",
            likes = "245K",
            comments = "1.2K",
            shares = "12K",
        ),
        DemoReel(
            id = "reel-2",
            author = DemoUser("city.frames", "City Frames", avatar(52)),
            image = photo("night-city", 1080, 1920),
            caption = "City lights never sleep \uD83C\uDF03",
            audio = "Original audio - city.frames",
            likes = "98K",
            comments = "742",
            shares = "5.3K",
        ),
        DemoReel(
            id = "reel-3",
            author = DemoUser("fit.life", "Fit Life", avatar(68)),
            image = photo("gym-session", 1080, 1920),
            caption = "Consistency beats motivation \uD83D\uDCAA",
            audio = "Original audio - fit.life",
            likes = "156K",
            comments = "3.1K",
            shares = "9.8K",
        ),
    )

    val exploreFilters = listOf("For you", "Travel", "Fashion", "Fitness", "Food")

    val exploreTiles = listOf(
        DemoTile(photo("forest-lake", 700, 900), "#nature"),
        DemoTile(photo("portrait-glow", 700, 900), "#aesthetic"),
        DemoTile(photo("kitten", 700, 900), "#cute"),
        DemoTile(photo("sports-car", 700, 900), "#cars"),
        DemoTile(photo("city-night", 700, 900), "#citylife"),
        DemoTile(photo("gym-weights", 700, 900), "#fitness"),
        DemoTile(photo("street-food", 700, 900), "#food"),
        DemoTile(photo("street-style", 700, 900), "#lifestyle"),
    )

    val searchSuggestions = listOf(
        "nature", "travel", "aesthetic", "instagood", "fitness", "food", "photography", "cars",
    )

    val suggestedAccounts = listOf(
        DemoUser("nature_vibes", "Nature & Travel", avatar(48)),
        DemoUser("fashion_daily", "Fashion & Style", avatar(25)),
        DemoUser("foodiee", "Food Lovers", avatar(31)),
        DemoUser("travel.diaries", "Travel The World", avatar(33)),
    )

    val chats = listOf(
        DemoChat(DemoUser("riya_07", "Riya", avatar(5)), "Hey! How are you?", "2m", unread = true),
        DemoChat(DemoUser("shivam_21", "Shivam", avatar(13)), "Seen 10m ago", "10m"),
        DemoChat(DemoUser("anushka", "Anushka", avatar(9)), "That's awesome!", "12m", unread = true),
        DemoChat(DemoUser("kartik", "Kartik", avatar(14)), "Bro, call karna", "25m"),
        DemoChat(DemoUser("priyanshu_03", "Priyanshu", avatar(15)), "Photo", "1h"),
        DemoChat(DemoUser("neha_singh", "Neha", avatar(20)), "Typing...", "1h"),
        DemoChat(DemoUser("aditya_official", "Aditya", avatar(51)), "Sent 2h ago", "2h"),
        DemoChat(DemoUser("muskan_17", "Muskan", avatar(44)), "Haha \uD83D\uDE02", "3h"),
    )

    val conversation = listOf(
        DemoMessage("Hey! How are you? \uD83D\uDE0A", "2:14 PM", mine = false),
        DemoMessage("I'm good! Just studying for exams. How about you?", "2:16 PM", mine = true),
        DemoMessage("Same here \uD83D\uDE05", "2:17 PM", mine = false),
        DemoMessage("All the best! You can do it! \uD83D\uDCAA", "2:18 PM", mine = true),
        DemoMessage("", "2:18 PM", mine = false, image = photo("golden-sunset", 800, 600)),
    )

    val notifications = listOf(
        DemoNotification(DemoUser("riya_07", "Riya", avatar(5)), "liked your post.", "2h", thumbnail = photo("mountain-trek", 200, 200)),
        DemoNotification(DemoUser("shivam_21", "Shivam", avatar(13)), "started following you.", "5h", follow = true),
        DemoNotification(DemoUser("anushka", "Anushka", avatar(9)), "commented: Nice pic!", "12h", thumbnail = photo("golden-sunset", 200, 200)),
        DemoNotification(DemoUser("kartik", "Kartik", avatar(14)), "liked your reel.", "20h", thumbnail = photo("ocean-sunset", 200, 200)),
        DemoNotification(DemoUser("neha_singh", "Neha", avatar(20)), "started following you.", "1d", follow = true),
        DemoNotification(DemoUser("aditya_official", "Aditya", avatar(51)), "liked your post.", "1d", thumbnail = photo("desk-setup", 200, 200)),
    )

    val highlights = listOf(
        DemoHighlight("Travel", photo("temple-trip", 300, 300)),
        DemoHighlight("Coding", photo("desk-setup", 300, 300)),
        DemoHighlight("Friends", photo("friends-group", 300, 300)),
        DemoHighlight("Life", photo("portrait-glow", 300, 300)),
    )

    val profileGrid = listOf(
        photo("desk-setup", 500, 500),
        photo("better-days", 500, 500),
        photo("neon-abstract", 500, 500),
        photo("mountain-trek", 500, 500),
        photo("code-screen", 500, 500),
        photo("palm-sunset", 500, 500),
        photo("forest-lake", 500, 500),
        photo("city-night", 500, 500),
        photo("ocean-sunset", 500, 500),
    )

    val filters = listOf("Normal", "Clarendon", "Gingham", "Moon", "Lark", "Juno")

    val composerPreview = photo("city-sunset", 1080, 1080)
}
