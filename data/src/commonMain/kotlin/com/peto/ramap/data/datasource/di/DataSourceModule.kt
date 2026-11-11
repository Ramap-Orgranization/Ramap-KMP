package com.peto.ramap.data.datasource.di

import com.peto.ramap.data.datasource.announcement.AnnouncementDataSource
import com.peto.ramap.data.datasource.announcement.AnnouncementReadDataSource
import com.peto.ramap.data.datasource.announcement.RemoteAnnouncementDataSource
import com.peto.ramap.data.datasource.announcement.RemoteAnnouncementReadDataSource
import com.peto.ramap.data.datasource.appnotice.AppNoticeDataSource
import com.peto.ramap.data.datasource.appnotice.RemoteAppNoticeDataSource
import com.peto.ramap.data.datasource.follow.FollowDataSource
import com.peto.ramap.data.datasource.follow.RemoteFollowDataSource
import com.peto.ramap.data.datasource.image.BatchingImageUrlDataSource
import com.peto.ramap.data.datasource.image.CachedImageUrlDataSource
import com.peto.ramap.data.datasource.image.ExpiredImageUrlParser
import com.peto.ramap.data.datasource.image.ImageUrlAccessPolicy
import com.peto.ramap.data.datasource.image.ImageUrlDataSource
import com.peto.ramap.data.datasource.image.RemoteImageUrlDataSource
import com.peto.ramap.data.datasource.importation.ImportationDataSource
import com.peto.ramap.data.datasource.importation.KakaoImportationDataSource
import com.peto.ramap.data.datasource.importation.NaverImportationDataSource
import com.peto.ramap.data.datasource.importation.RemoteImportationDataSource
import com.peto.ramap.data.datasource.menu.RemoteShopMenuDataSource
import com.peto.ramap.data.datasource.menu.ShopMenuDataSource
import com.peto.ramap.data.datasource.moderation.RemoteUserModerationDataSource
import com.peto.ramap.data.datasource.moderation.UserModerationDataSource
import com.peto.ramap.data.datasource.notice.OperatingNoticeDataSource
import com.peto.ramap.data.datasource.notice.RemoteOperatingNoticeDataSource
import com.peto.ramap.data.datasource.notification.PushRegistrationDataSource
import com.peto.ramap.data.datasource.notification.RemotePushRegistrationDataSource
import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.data.datasource.personalization.NotificationDataSource
import com.peto.ramap.data.datasource.personalization.RemoteBookmarkShopDataSource
import com.peto.ramap.data.datasource.personalization.RemoteNotificationDataSource
import com.peto.ramap.data.datasource.place.PlaceSearchDataSource
import com.peto.ramap.data.datasource.place.RemotePlaceSearchDataSource
import com.peto.ramap.data.datasource.profile.ProfileDataSource
import com.peto.ramap.data.datasource.profile.ProfileVisitRankingDataSource
import com.peto.ramap.data.datasource.profile.RemoteProfileDataSource
import com.peto.ramap.data.datasource.profile.RemoteProfileVisitRankingDataSource
import com.peto.ramap.data.datasource.profile.RemoteUserProfileDataSource
import com.peto.ramap.data.datasource.profile.UserProfileDataSource
import com.peto.ramap.data.datasource.ranking.RemoteShopRankingDataSource
import com.peto.ramap.data.datasource.ranking.ShopRankingDataSource
import com.peto.ramap.data.datasource.report.RemotePlaceLinkResolver
import com.peto.ramap.data.datasource.report.RemoteShopReportDataSource
import com.peto.ramap.data.datasource.report.ShopReportDataSource
import com.peto.ramap.data.datasource.review.RemoteReviewDataSource
import com.peto.ramap.data.datasource.review.ReviewDataSource
import com.peto.ramap.data.datasource.review.ReviewImageResolver
import com.peto.ramap.data.datasource.review.ReviewImageUrlAccessPolicy
import com.peto.ramap.data.datasource.shop.RamenShopDataSource
import com.peto.ramap.data.datasource.shop.RemoteRamenShopDataSource
import com.peto.ramap.data.datasource.update.AppUpdatePolicyDataSource
import com.peto.ramap.data.datasource.update.RemoteAppUpdatePolicyDataSource
import com.peto.ramap.data.datasource.waiting.RemoteShopWaitingSystemDataSource
import com.peto.ramap.data.datasource.waiting.ShopWaitingSystemDataSource
import com.peto.ramap.data.feed.FeedDataSource
import com.peto.ramap.data.feed.RemoteFeedDataSource
import com.peto.ramap.domain.repository.PlaceLinkResolver
import com.peto.ramap.domain.store.ReviewChangeStore
import com.peto.ramap.network.config.RamapSecrets
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataSourceModule =
    module {
        single<AnnouncementDataSource> { RemoteAnnouncementDataSource(get()) }
        single<AnnouncementReadDataSource> { RemoteAnnouncementReadDataSource(get()) }
        single<PushRegistrationDataSource> { RemotePushRegistrationDataSource(get()) }
        single<NotificationDataSource> { RemoteNotificationDataSource(get()) }
        single<FollowDataSource> { RemoteFollowDataSource(get(), get()) }
        single<ImageUrlAccessPolicy> { ReviewImageUrlAccessPolicy(get(), lazy { get<ReviewChangeStore>() }) }
        single<ImageUrlDataSource> { CachedImageUrlDataSource(RemoteImageUrlDataSource(get(), get()), get()) }
        single { BatchingImageUrlDataSource(get(), get(), get()) }
        single {
            ExpiredImageUrlParser(
                supabaseSignedBaseUrl = "${RamapSecrets.supabaseUrl.trimEnd('/')}/storage/v1/object/sign",
                r2AccountId = RamapSecrets.r2AccountId,
                r2PrivateBucket = RamapSecrets.r2PrivateBucket,
            )
        }
        singleOf(::ReviewImageResolver)
        single<ProfileDataSource> { RemoteProfileDataSource(get(), get(), get()) }
        single<UserProfileDataSource> { RemoteUserProfileDataSource(get(), get(), get()) }
        single<ProfileVisitRankingDataSource> { RemoteProfileVisitRankingDataSource(get()) }
        single<UserModerationDataSource> { RemoteUserModerationDataSource(get(), get()) }
        single<ReviewDataSource> { RemoteReviewDataSource(get(), get(), get()) }
        single<FeedDataSource> { RemoteFeedDataSource(get(), get()) }
        single { NaverImportationDataSource(get(), get<HttpClient>()) }
        single { KakaoImportationDataSource(get()) }
        single<ImportationDataSource> { RemoteImportationDataSource(get(), get()) }
        single<PlaceSearchDataSource> {
            RemotePlaceSearchDataSource(get<SupabaseClient>())
        }
        single<RamenShopDataSource> {
            RemoteRamenShopDataSource(get<SupabaseClient>())
        }
        single<ShopMenuDataSource> { RemoteShopMenuDataSource(get<SupabaseClient>()) }
        single<OperatingNoticeDataSource> {
            RemoteOperatingNoticeDataSource(get<SupabaseClient>())
        }
        single<ShopRankingDataSource> {
            RemoteShopRankingDataSource(get<SupabaseClient>())
        }
        single<ShopWaitingSystemDataSource> {
            RemoteShopWaitingSystemDataSource(get<SupabaseClient>())
        }
        single<BookmarkShopDataSource> {
            RemoteBookmarkShopDataSource(get<SupabaseClient>())
        }
        single<ShopReportDataSource> {
            RemoteShopReportDataSource(get<SupabaseClient>())
        }
        single<PlaceLinkResolver> { RemotePlaceLinkResolver(get<HttpClient>()) }
        single<AppUpdatePolicyDataSource> {
            RemoteAppUpdatePolicyDataSource(get<SupabaseClient>())
        }
        single<AppNoticeDataSource> {
            RemoteAppNoticeDataSource(get<SupabaseClient>())
        }
    }
