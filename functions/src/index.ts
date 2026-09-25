/**
 * Insangram Cloud Functions entry point.
 *
 * Everything a client is not allowed to do lives here: trusted counters,
 * notification fan-out, username uniqueness, scheduled cleanup and moderation
 * hooks. Security rules deny those writes from devices, so these functions are
 * the single writer for anything that must be trustworthy.
 */

export {
  onPostLikeCreated,
  onPostLikeDeleted,
  onReelLikeCreated,
  onReelLikeDeleted,
  onCommentCreated,
  onCommentDeleted,
  onCommentLikeWritten,
  onSavedPostWritten,
  onPostWritten,
  onStoryViewCreated,
} from "./counters";

export {
  onFollowCreated,
  onFollowUpdated,
  onFollowDeleted,
  reserveUsername,
  onMessageCreated,
  onStoryReactionCreated,
  onReportCreated,
  recordView,
  recordShare,
  recordProfileVisit,
} from "./social";

export {
  expireStories,
  purgeRecentlyDeleted,
  cleanupOrphanedMedia,
  pruneStaleTokens,
  expireNotesAndTrimNotifications,
  onUserDocumentDeleted,
  grantModeratorClaim,
} from "./maintenance";
