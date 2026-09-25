package com.insangram.app

import com.insangram.app.domain.model.FollowState
import com.insangram.app.domain.usecase.VisibilityPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Client-side mirror of the Firestore rules. The server remains authoritative;
 * this exists so the UI never offers an action the backend would reject.
 */
class VisibilityPolicyTest {

    private val policy = VisibilityPolicy()

    @Test
    fun `public accounts are visible to any signed in viewer`() {
        assertTrue(
            policy.canViewContent(
                viewerId = "viewer",
                authorId = "author",
                authorIsPrivate = false,
                followState = FollowState.NOT_FOLLOWING,
                blockedEitherWay = false,
            ),
        )
    }

    @Test
    fun `private accounts hide content from unapproved viewers`() {
        assertFalse(
            policy.canViewContent(
                viewerId = "viewer",
                authorId = "author",
                authorIsPrivate = true,
                followState = FollowState.NOT_FOLLOWING,
                blockedEitherWay = false,
            ),
        )
        assertFalse(
            policy.canViewContent(
                viewerId = "viewer",
                authorId = "author",
                authorIsPrivate = true,
                followState = FollowState.REQUESTED,
                blockedEitherWay = false,
            ),
        )
    }

    @Test
    fun `approved followers see private content`() {
        assertTrue(
            policy.canViewContent(
                viewerId = "viewer",
                authorId = "author",
                authorIsPrivate = true,
                followState = FollowState.FOLLOWING,
                blockedEitherWay = false,
            ),
        )
    }

    @Test
    fun `a block hides content in both directions even for public accounts`() {
        assertFalse(
            policy.canViewContent(
                viewerId = "viewer",
                authorId = "author",
                authorIsPrivate = false,
                followState = FollowState.FOLLOWING,
                blockedEitherWay = true,
            ),
        )
    }

    @Test
    fun `owners always see their own content`() {
        assertTrue(
            policy.canViewContent(
                viewerId = "author",
                authorId = "author",
                authorIsPrivate = true,
                followState = FollowState.SELF,
                blockedEitherWay = false,
            ),
        )
    }
}
