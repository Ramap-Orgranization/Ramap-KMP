package com.peto.ramap.ui.review.detail

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.coroutinesTest
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.moderation.ReportReason
import com.peto.ramap.domain.model.review.Review
import com.peto.ramap.domain.model.review.ReviewAuthor
import com.peto.ramap.domain.model.review.ReviewDetail
import com.peto.ramap.domain.model.review.change.ReviewChange
import com.peto.ramap.domain.model.review.comment.ReviewComment
import com.peto.ramap.domain.model.review.comment.ReviewCommentCursor
import com.peto.ramap.domain.model.review.comment.ReviewCommentDeletion
import com.peto.ramap.domain.model.review.comment.ReviewCommentPage
import com.peto.ramap.domain.model.review.comment.ReviewCommentReceipt
import com.peto.ramap.domain.model.review.comment.ReviewCommentSubmission
import com.peto.ramap.domain.model.review.comment.ReviewCommentThread
import com.peto.ramap.domain.repository.ReviewCommentRepository
import com.peto.ramap.fake.FakeLoginRepository
import com.peto.ramap.fake.FakeReviewChangeStore
import com.peto.ramap.fake.FakeReviewNotificationRepository
import com.peto.ramap.fake.FakeUserModerationRepository
import com.peto.ramap.ui.review.detail.contract.ReviewDetailIntent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewDetailViewModelTest {
    @Test
    fun `실패 후 같은 요청 ID로 재시도해 이전 페이지 원댓글의 답글이 즉시 표시된다`() =
        coroutinesTest {
            val fake = ReviewCommentsFixture()
            val viewModel = viewModel(fake)
            runCurrent()
            viewModel.dispatch(ReviewDetailIntent.LoadMoreComments)
            runCurrent()
            viewModel.dispatch(ReviewDetailIntent.ReplyTo(fake.older.comment))
            viewModel.dispatch(ReviewDetailIntent.ChangeCommentDraft("새 답글"))
            viewModel.dispatch(ReviewDetailIntent.Submit)
            runCurrent()

            assertTrue(viewModel.uiState.value.submitFailed)
            assertEquals("새 답글", viewModel.uiState.value.draft)

            viewModel.dispatch(ReviewDetailIntent.Submit)
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(fake.submissions.first(), fake.submissions.last())
            assertEquals(listOf("newer", "older"), state.threads.map { it.comment.id })
            assertEquals(
                listOf("loaded-reply", "created"),
                state.threads
                    .last()
                    .replies
                    .map { it.id },
            )
            assertEquals(fake.older.nextReplyCursor, state.threads.last().nextReplyCursor)
            assertEquals(5, state.detail?.review?.commentCount)
            assertEquals("", state.draft)
            assertNull(state.replyTarget)
        }

    @Test
    fun `로그인 중에는 리뷰와 댓글을 유지하고 조회가 끝나면 현재 계정의 권한을 반영한다`() =
        coroutinesTest {
            val fake = ReviewCommentsFixture().apply { canWriteComments = false }
            val changes = FakeReviewChangeStore().apply { reset(null) }
            val login = FakeLoginRepository()
            val viewModel = viewModel(fake, changes, login)
            runCurrent()

            val guestState = viewModel.uiState.value
            val guestDetail = requireNotNull(guestState.detail)
            assertFalse(guestDetail.canWriteComments)
            assertTrue(guestState.commentsLoaded)

            val detailResult = CompletableDeferred<RamapResult<ReviewDetail?>>()
            val commentsResult = CompletableDeferred<RamapResult<ReviewCommentPage>>()
            fake.detailResult = { detailResult.await() }
            fake.firstCommentsResult = { commentsResult.await() }
            login.updateAccountId("owner")
            changes.reset("owner")
            runCurrent()

            val refreshing = viewModel.uiState.value
            assertTrue(refreshing.isRefreshingSession)
            assertEquals(guestDetail, refreshing.detail)
            assertEquals(guestState.threads, refreshing.threads)

            val signedInDetail = guestDetail.copy(canWriteComments = true, reviewNotificationsEnabled = true)
            detailResult.complete(RamapResult.Success(signedInDetail))
            runCurrent()
            assertTrue(viewModel.uiState.value.isRefreshingSession)
            assertEquals(guestState.threads, viewModel.uiState.value.threads)

            val ownedThread = fake.newer.copy(comment = fake.newer.comment.copy(canManage = true))
            commentsResult.complete(RamapResult.Success(ReviewCommentPage(listOf(ownedThread), null, 1)))
            runCurrent()

            val state = viewModel.uiState.value
            assertFalse(state.isRefreshingSession)
            assertEquals("owner", state.viewerId)
            assertTrue(state.detail?.canWriteComments == true)
            assertEquals(true, state.detail?.reviewNotificationsEnabled)
            assertEquals(listOf(ownedThread), state.threads)
            assertEquals(1, state.detail?.review?.commentCount)
        }

    @Test
    fun `로그인한 계정에 공개되지 않는 리뷰는 이전에 보던 리뷰와 댓글을 지운다`() =
        coroutinesTest {
            val fake = ReviewCommentsFixture().apply { canWriteComments = false }
            val changes = FakeReviewChangeStore().apply { reset(null) }
            val login = FakeLoginRepository()
            val viewModel = viewModel(fake, changes, login)
            runCurrent()
            assertTrue(
                viewModel.uiState.value.threads
                    .isNotEmpty(),
            )

            fake.detailResult = { RamapResult.Success(null) }
            login.updateAccountId("owner")
            changes.reset("owner")
            runCurrent()

            val state = viewModel.uiState.value
            assertNull(state.detail)
            assertTrue(state.threads.isEmpty())
            assertTrue(state.unavailable)
            assertFalse(state.isRefreshingSession)
        }

    @Test
    fun `계정이 바뀌면 이전 초안과 진행 중인 작성 결과를 표시하지 않는다`() =
        coroutinesTest {
            val fake = ReviewCommentsFixture()
            val changes = FakeReviewChangeStore()
            val login = FakeLoginRepository(LoginSessionState.AUTHENTICATED, initialAccountId = "owner")
            val viewModel = viewModel(fake, changes, login)
            val pending = CompletableDeferred<RamapResult<ReviewCommentReceipt>>()
            fake.submitResult = { pending.await() }
            runCurrent()
            viewModel.dispatch(ReviewDetailIntent.ChangeCommentDraft("이전 계정 초안"))
            viewModel.dispatch(ReviewDetailIntent.Submit)
            runCurrent()
            assertTrue(viewModel.uiState.value.isSubmitting)

            login.updateAccountId("other")
            changes.reset("other")
            runCurrent()
            pending.complete(RamapResult.Success(ReviewCommentReceipt(comment("old-session"), 9)))
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals("other", state.viewerId)
            assertEquals("", state.draft)
            assertEquals(listOf("newer"), state.threads.map { it.comment.id })
            assertEquals(4, state.detail?.review?.commentCount)
        }

    @Test
    fun `차단으로 접근이 바뀌면 펼친 댓글과 답글을 비운다`() =
        coroutinesTest {
            val fake = ReviewCommentsFixture()
            val changes = FakeReviewChangeStore()
            val viewModel = viewModel(fake, changes, showDetail = false)
            runCurrent()
            assertEquals(
                listOf("newer"),
                viewModel.uiState.value.threads
                    .map { it.comment.id },
            )

            fake.accessDenied = true
            changes.publish(ReviewChange.AccessChanged("blocked"))
            runCurrent()

            assertTrue(viewModel.uiState.value.unavailable)
            assertTrue(
                viewModel.uiState.value.threads
                    .isEmpty(),
            )
        }

    @Test
    fun `답글 신고는 해당 답글과 현재 계정으로 접수한다`() =
        coroutinesTest {
            val fake = ReviewCommentsFixture()
            val viewModel = viewModel(fake)
            runCurrent()
            viewModel.dispatch(ReviewDetailIntent.LoadMoreComments)
            runCurrent()

            viewModel.dispatch(ReviewDetailIntent.Report(ReportReason.SPAM, "", "loaded-reply"))
            runCurrent()

            assertEquals(listOf("loaded-reply" to "owner"), fake.commentReports)
        }

    @Test
    fun `작성자만 이 리뷰의 알림을 끌 수 있다`() =
        coroutinesTest {
            val fake = ReviewCommentsFixture().apply { notificationsEnabled = true }
            val notifications = FakeReviewNotificationRepository()
            val viewModel = viewModel(fake, notifications = notifications)
            runCurrent()

            viewModel.dispatch(ReviewDetailIntent.ToggleNotifications)
            runCurrent()

            assertEquals(listOf("review" to false), notifications.reviewUpdates)
            assertEquals(
                false,
                viewModel.uiState.value.detail
                    ?.reviewNotificationsEnabled,
            )
        }

    @Test
    fun `수정한 댓글을 같은 위치에 표시하고 이미 펼친 답글을 유지한다`() =
        coroutinesTest {
            val fake = ReviewCommentsFixture()
            val viewModel = viewModel(fake)
            runCurrent()
            viewModel.dispatch(ReviewDetailIntent.LoadMoreComments)
            runCurrent()

            viewModel.dispatch(ReviewDetailIntent.ReplyTo(fake.older.comment))
            viewModel.dispatch(ReviewDetailIntent.ChangeCommentDraft("작성 중인 답글"))
            viewModel.dispatch(ReviewDetailIntent.StartCommentEdit("older"))
            runCurrent()
            assertEquals(fake.older.comment.body, viewModel.uiState.value.composerDraft)

            viewModel.dispatch(ReviewDetailIntent.ChangeCommentDraft("취소할 수정", editingCommentId = "older"))
            viewModel.dispatch(ReviewDetailIntent.CancelCommentEdit)
            runCurrent()
            assertEquals("작성 중인 답글", viewModel.uiState.value.composerDraft)

            viewModel.dispatch(ReviewDetailIntent.StartCommentEdit("older"))
            viewModel.dispatch(ReviewDetailIntent.ChangeCommentDraft("고친 댓글", editingCommentId = "older"))
            viewModel.dispatch(ReviewDetailIntent.SaveCommentEdit("older"))
            runCurrent()

            val state = viewModel.uiState.value
            val thread = state.threads.last()
            assertEquals("고친 댓글", thread.comment.body)
            assertTrue(thread.comment.isEdited)
            assertEquals(fake.older.replies, thread.replies)
            assertEquals(listOf("older"), fake.edits)
            assertTrue(fake.submissions.isEmpty())
            assertNull(state.commentEdit)
            assertEquals("작성 중인 답글", state.composerDraft)
            assertEquals("older", state.replyTarget?.id)
        }

    @Test
    fun `답글 수정에 실패하면 고친 내용을 유지하고 같은 답글에 다시 저장한다`() =
        coroutinesTest {
            val fake = ReviewCommentsFixture(manageLoadedReply = true).apply { editFails = true }
            val viewModel = viewModel(fake)
            runCurrent()
            viewModel.dispatch(ReviewDetailIntent.LoadMoreComments)
            runCurrent()

            viewModel.dispatch(ReviewDetailIntent.StartCommentEdit("loaded-reply"))
            runCurrent()
            assertEquals(
                fake.older.replies
                    .single()
                    .body,
                viewModel.uiState.value.composerDraft,
            )

            viewModel.dispatch(ReviewDetailIntent.ChangeCommentDraft("고친 답글", editingCommentId = "loaded-reply"))
            viewModel.dispatch(ReviewDetailIntent.SaveCommentEdit("loaded-reply"))
            runCurrent()
            assertEquals("고친 답글", viewModel.uiState.value.composerDraft)
            assertEquals(
                fake.older.replies.single(),
                viewModel.uiState.value.threads
                    .last()
                    .replies
                    .single(),
            )

            fake.editFails = false
            viewModel.dispatch(ReviewDetailIntent.SaveCommentEdit("loaded-reply"))
            runCurrent()

            val state = viewModel.uiState.value
            val reply =
                state.threads
                    .last()
                    .replies
                    .single()
            assertEquals("고친 답글", reply.body)
            assertTrue(reply.isEdited)
            assertEquals(listOf("loaded-reply", "loaded-reply"), fake.edits)
            assertNull(state.commentEdit)
        }

    @Test
    fun `답글이 남은 댓글을 삭제하면 본문을 지우고 답글을 유지한다`() =
        coroutinesTest {
            val fake = ReviewCommentsFixture()
            val viewModel = viewModel(fake)
            runCurrent()
            viewModel.dispatch(ReviewDetailIntent.LoadMoreComments)
            runCurrent()

            viewModel.dispatch(ReviewDetailIntent.DeleteComment("older"))
            runCurrent()

            val state = viewModel.uiState.value
            val thread = state.threads.last()
            assertTrue(thread.comment.isDeleted)
            assertEquals("", thread.comment.body)
            assertEquals(fake.older.replies, thread.replies)
            assertEquals(3, state.detail?.review?.commentCount)
            assertEquals(listOf("older"), fake.deletions)
        }

    @Test
    fun `다른 사람의 댓글은 수정 또는 삭제 요청을 보내지 않는다`() =
        coroutinesTest {
            val fake = ReviewCommentsFixture()
            val viewModel = viewModel(fake)
            runCurrent()

            viewModel.dispatch(ReviewDetailIntent.StartCommentEdit("newer"))
            viewModel.dispatch(ReviewDetailIntent.SaveCommentEdit("newer"))
            viewModel.dispatch(ReviewDetailIntent.DeleteComment("newer"))
            runCurrent()

            assertNull(viewModel.uiState.value.commentEdit)
            assertTrue(fake.edits.isEmpty())
            assertTrue(fake.deletions.isEmpty())
        }

    private fun viewModel(
        comments: ReviewCommentRepository,
        changes: FakeReviewChangeStore = FakeReviewChangeStore(),
        login: FakeLoginRepository = FakeLoginRepository(LoginSessionState.AUTHENTICATED, initialAccountId = "owner"),
        showDetail: Boolean = true,
        notifications: FakeReviewNotificationRepository = FakeReviewNotificationRepository(),
    ): ReviewDetailViewModel =
        ReviewDetailViewModel(
            reviewId = "review",
            showDetail = showDetail,
            comments = comments,
            reviews = UnusedReviewRepository,
            moderation = FakeUserModerationRepository(),
            login = login,
            changes = changes,
            notifications = notifications,
        )
}

internal class ReviewCommentsFixture(
    manageLoadedReply: Boolean = false,
) : ReviewCommentRepository {
    val newer = ReviewCommentThread(comment("newer"), emptyList(), null)
    val older =
        comment("older").copy(canManage = true).let { root ->
            val reply =
                comment("loaded-reply", parent = root.id, createdAt = "2026-11-11T08:00:00Z")
                    .copy(canManage = manageLoadedReply, canReport = !manageLoadedReply)
            ReviewCommentThread(root, listOf(reply), reply.cursor)
        }
    val edits = mutableListOf<String>()
    val deletions = mutableListOf<String>()
    val commentReports = mutableListOf<Pair<String, String>>()
    var notificationsEnabled: Boolean? = null
    var canWriteComments = true
    var detailResult: (suspend () -> RamapResult<ReviewDetail?>)? = null
    var firstCommentsResult: (suspend () -> RamapResult<ReviewCommentPage>)? = null
    val submissions = mutableListOf<ReviewCommentSubmission>()
    var accessDenied = false
    var editFails = false
    var submitResult: suspend (ReviewCommentSubmission) -> RamapResult<ReviewCommentReceipt> = {
        if (submissions.size == 1) {
            RamapResult.Error(RamapError.Timeout(IllegalStateException("응답 시간 초과")))
        } else {
            RamapResult.Success(ReviewCommentReceipt(comment("created", it.parentId, "2026-11-11T08:00:02Z"), 5))
        }
    }

    override suspend fun fetchReviewDetail(reviewId: String): RamapResult<ReviewDetail?> =
        detailResult?.invoke() ?: RamapResult.Success(
            ReviewDetail(
                Review("review", "shop", "리뷰", "2026-11-11T08:00:00Z", author = ReviewAuthor("owner", "작성자")),
                "라멘집",
                canLike = true,
                canWriteComments = canWriteComments,
                reviewNotificationsEnabled = notificationsEnabled,
            ),
        )

    override suspend fun fetchComments(
        reviewId: String,
        parentId: String?,
        cursor: ReviewCommentCursor?,
    ): RamapResult<ReviewCommentPage> =
        when {
            accessDenied -> RamapResult.Error(RamapError.Http(403))
            cursor == null -> firstCommentsResult?.invoke() ?: RamapResult.Success(ReviewCommentPage(listOf(newer), newer.comment.cursor, 4))
            else -> RamapResult.Success(ReviewCommentPage(listOf(older), null, 4))
        }

    override suspend fun editComment(
        reviewId: String,
        commentId: String,
        body: String,
        expectedAuthorId: String,
    ): RamapResult<ReviewCommentReceipt> {
        edits += commentId
        if (editFails) return RamapResult.Error(RamapError.Http(500))
        val target = (listOf(older.comment) + older.replies).first { it.id == commentId }
        return RamapResult.Success(ReviewCommentReceipt(target.copy(body = body, isEdited = true), 4))
    }

    override suspend fun deleteComment(
        reviewId: String,
        commentId: String,
        expectedAuthorId: String,
    ): RamapResult<ReviewCommentDeletion> {
        deletions += commentId
        return RamapResult.Success(ReviewCommentDeletion(commentCount = 3, keepThread = true))
    }

    override suspend fun reportComment(
        commentId: String,
        reason: ReportReason,
        details: String,
        expectedAccountId: String,
    ): RamapResult<Unit> {
        commentReports += commentId to expectedAccountId
        return RamapResult.Success(Unit)
    }

    override suspend fun submitComment(submission: ReviewCommentSubmission): RamapResult<ReviewCommentReceipt> {
        submissions += submission
        return submitResult(submission)
    }
}

private fun comment(
    id: String,
    parent: String? = null,
    createdAt: String = "2026-11-11T08:00:00Z",
): ReviewComment = ReviewComment(id, parent, "댓글", "작성자", createdAt, canReport = parent != null)
