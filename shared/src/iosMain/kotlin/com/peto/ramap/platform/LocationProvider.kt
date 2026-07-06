package com.peto.ramap.platform

import com.peto.ramap.domain.model.Location
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyHundredMeters
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * iOS CoreLocation에서 현재 위치를 조회해 공통 [Location] 모델로 반환한다.
 *
 * `CLLocationManager`는 iOS가 제공하는 위치 서비스 객체다. 이 객체는 위치 요청 결과를 즉시
 * 반환하지 않고 delegate callback으로 알려준다. 그래서 이 provider는
 * [CLLocationManagerDelegateProtocol]을 구현해 위치 성공/실패 callback을 받고,
 * 그 callback을 [position]의 suspend 결과로 이어준다.
 *
 * [NSObject]는 Objective-C 런타임과 연결되는 기본 클래스다. CoreLocation delegate는 iOS SDK의
 * Objective-C protocol이므로, Kotlin/Native에서 delegate 객체로 등록하려면 NSObject를 상속한
 * 객체가 해당 protocol을 구현해야 한다.
 *
 * 권한 요청과 권한 상태 판단은 호출자가 담당한다. 이 provider는 권한이 허용된 뒤 위치 값을
 * 가져오는 책임만 가진다.
 */
internal actual class LocationProvider :
    NSObject(),
    CLLocationManagerDelegateProtocol {
    private val locationManager = CLLocationManager()
    private var continuation: CancellableContinuation<Location?>? = null

    init {
        locationManager.delegate = this
        locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
    }

    /**
     * 캐시된 iOS 위치가 있으면 즉시 반환하고, 없으면 단발 위치 요청을 suspend로 기다린다.
     */
    actual suspend fun position(): Location? {
        locationManager.location?.let { location ->
            return location.toDomainLocation()
        }

        return suspendCancellableCoroutine { continuation ->
            this.continuation = continuation
            locationManager.requestLocation()

            continuation.invokeOnCancellation {
                this.continuation = null
            }
        }
    }

    /**
     * CoreLocation이 위치 조회에 성공했을 때 호출되는 delegate callback이다.
     */
    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        continuation?.resume(location?.toDomainLocation())
        continuation = null
    }

    /**
     * CoreLocation이 위치 조회에 실패했을 때 호출되는 delegate callback이다.
     */
    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError,
    ) {
        continuation?.resume(null)
        continuation = null
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun CLLocation.toDomainLocation(): Location =
        coordinate.useContents {
            Location(
                lat = latitude,
                lng = longitude,
            )
        }
}
