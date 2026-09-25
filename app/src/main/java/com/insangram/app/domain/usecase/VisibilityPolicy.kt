package com.insangram.app.domain.usecase

import com.insangram.app.domain.model.AudienceScope
import com.insangram.app.domain.model.FollowState
import com.insangram.app.domain.model.RelationshipSummary
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Client-side mirror of the Firestore security rules. The server rules are the
 * real boundary; this exists so the UI does not offer actions that would be
 * rejected, and so demo mode enforces the same behaviour offline.
 */
@Singleton
class VisibilityPolicy @Inject constructor() {

    fun canViewContent(
        isOwnContent: Boolean,
        authorIsPrivate: Boolean,
        relationship: RelationshipSummary,
    ): Boolean = when {
        isOwnContent -> true
        relationship.isBlockedByViewer -> false
        relationship.followState == FollowState.BLOCKED -> false
        !authorIsPrivate -> true
        else -> relationship.followState == FollowState.FOLLOWING
    }

    fun canViewStory(
        isOwnStory: Boolean,
        audience: AudienceScope,
        relationship: RelationshipSummary,
    ): Boolean = when {
        isOwnStory -> true
        relationship.isBlockedByViewer || relationship.followState == FollowState.BLOCKED -> false
        audience == AudienceScope.NOBODY -> false
        audience == AudienceScope.CLOSE_FRIENDS -> relationship.isCloseFriend
        audience == AudienceScope.FOLLOWERS -> relationship.followState == FollowState.FOLLOWING
        else -> true
    }

    fun canComment(
        commentsEnabled: Boolean,
        audience: AudienceScope,
        relationship: RelationshipSummary,
        isOwnContent: Boolean,
    ): Boolean = when {
        isOwnContent -> commentsEnabled
        !commentsEnabled -> false
        relationship.isBlockedByViewer || relationship.followState == FollowState.BLOCKED -> false
        relationship.isRestricted -> false
        audience == AudienceScope.NOBODY -> false
        audience == AudienceScope.FOLLOWERS -> relationship.followsViewer ||
            relationship.followState == FollowState.FOLLOWING
        audience == AudienceScope.CLOSE_FRIENDS -> relationship.isCloseFriend
        else -> true
    }

    fun canMessage(audience: AudienceScope, relationship: RelationshipSummary): Boolean = when {
        relationship.isBlockedByViewer || relationship.followState == FollowState.BLOCKED -> false
        audience == AudienceScope.NOBODY -> false
        audience == AudienceScope.FOLLOWERS -> relationship.followsViewer
        audience == AudienceScope.CLOSE_FRIENDS -> relationship.isCloseFriend
        else -> true
    }

    /** Owner-only surfaces: saved posts, archive, analytics, recently deleted. */
    fun isOwnerOnlySurfaceVisible(viewerId: String?, ownerId: String): Boolean =
        viewerId != null && viewerId == ownerId
}

/**
 * Encapsulates the legal follow-state transitions so the follow button and the
 * repositories cannot disagree, and duplicate follows/requests are impossible.
 */
@Singleton
class FollowStateUseCase @Inject constructor() {

    /** What tapping the follow button should do next, given the current state. */
    fun nextAction(current: FollowState, targetIsPrivate: Boolean): FollowAction = when (current) {
        FollowState.SELF -> FollowAction.NONE
        FollowState.BLOCKED -> FollowAction.NONE
        FollowState.FOLLOWING -> FollowAction.UNFOLLOW
        FollowState.REQUESTED -> FollowAction.CANCEL_REQUEST
        FollowState.NOT_FOLLOWING -> if (targetIsPrivate) FollowAction.REQUEST else FollowAction.FOLLOW
    }

    /** The optimistic state to render immediately after [action]. */
    fun optimisticState(current: FollowState, action: FollowAction): FollowState = when (action) {
        FollowAction.FOLLOW -> FollowState.FOLLOWING
        FollowAction.REQUEST -> FollowState.REQUESTED
        FollowAction.UNFOLLOW, FollowAction.CANCEL_REQUEST -> FollowState.NOT_FOLLOWING
        FollowAction.NONE -> current
    }

    fun affectsFollowerCount(action: FollowAction): Long = when (action) {
        FollowAction.FOLLOW -> 1L
        FollowAction.UNFOLLOW -> -1L
        else -> 0L
    }
}

enum class FollowAction { FOLLOW, REQUEST, UNFOLLOW, CANCEL_REQUEST, NONE }
