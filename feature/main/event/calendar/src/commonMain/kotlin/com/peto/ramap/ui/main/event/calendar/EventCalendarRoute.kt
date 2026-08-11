package com.peto.ramap.ui.main.event.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.peto.ramap.designsystem.component.LoadErrorContent
import com.peto.ramap.designsystem.indicator.RamenLoadingIndicator
import com.peto.ramap.designsystem.text.AppText
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.extension.noRippleClickable
import com.peto.ramap.theme.AppTextStyle
import com.peto.ramap.theme.CommonColor
import com.peto.ramap.theme.GrayColor
import com.peto.ramap.theme.RamapTheme
import com.peto.ramap.ui.main.event.calendar.component.CalendarDayCell
import com.peto.ramap.ui.main.event.calendar.component.CalendarEventsBottomSheet
import com.peto.ramap.ui.main.event.calendar.component.CalendarLegend
import com.peto.ramap.ui.main.event.calendar.contract.EventCalendarIntent
import com.peto.ramap.ui.main.event.calendar.contract.EventCalendarUiState
import com.peto.ramap.ui.main.event.calendar.model.CalendarDaySelection
import com.peto.ramap.ui.main.event.calendar.model.CalendarMonth
import com.peto.ramap.ui.main.event.calendar.preview.EventCalendarPreviewParameterProvider
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import ramap.shared.generated.resources.Res
import ramap.shared.generated.resources.event_calendar_month_header
import ramap.shared.generated.resources.event_calendar_next_month
import ramap.shared.generated.resources.event_calendar_previous_month
import ramap.shared.generated.resources.event_calendar_weekday_friday
import ramap.shared.generated.resources.event_calendar_weekday_monday
import ramap.shared.generated.resources.event_calendar_weekday_saturday
import ramap.shared.generated.resources.event_calendar_weekday_sunday
import ramap.shared.generated.resources.event_calendar_weekday_thursday
import ramap.shared.generated.resources.event_calendar_weekday_tuesday
import ramap.shared.generated.resources.event_calendar_weekday_wednesday
import ramap.shared.generated.resources.event_list_error_description
import ramap.shared.generated.resources.event_list_error_title
import ramap.shared.generated.resources.event_list_open
import ramap.shared.generated.resources.ic_chevron_left
import ramap.shared.generated.resources.ic_chevron_right
import ramap.shared.generated.resources.ic_list
import ramap.shared.generated.resources.laduck_error_crying

@Composable
fun EventCalendarRoute(
    onBack: () -> Unit,
    onEventClick: (String) -> Unit,
    viewModel: EventCalendarViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    EventCalendarScreen(
        uiState = uiState,
        onListClick = onBack,
        onPreviousMonthClick = { viewModel.dispatch(EventCalendarIntent.OnPreviousMonthClicked) },
        onNextMonthClick = { viewModel.dispatch(EventCalendarIntent.OnNextMonthClicked) },
        onRetryClick = { viewModel.dispatch(EventCalendarIntent.OnRetryClicked) },
        onRefresh = { viewModel.dispatch(EventCalendarIntent.OnRefreshClicked) },
        onEventClick = { event -> onEventClick(event.id) },
    )
}

@Composable
internal fun EventCalendarScreen(
    uiState: EventCalendarUiState,
    onListClick: () -> Unit,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onRetryClick: () -> Unit,
    onEventClick: (ShopEvent) -> Unit,
    onRefresh: () -> Unit = {},
) {
    var selectedDay by remember { mutableStateOf<CalendarDaySelection?>(null) }
    val pullToRefreshState = rememberPullToRefreshState()
    Box(
        modifier =
            Modifier
                .statusBarsPadding()
                .fillMaxSize()
                .background(CommonColor.White),
    ) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = CommonColor.White,
                    color = CommonColor.Black,
                )
            },
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading ->
                        Column(modifier = Modifier.fillMaxSize()) {
                            CalendarTopBar(
                                month = uiState.month,
                                hasPreviousMonthEvents = uiState.hasPreviousMonthEvents,
                                hasNextMonthEvents = uiState.hasNextMonthEvents,
                                onPreviousMonthClick = onPreviousMonthClick,
                                onNextMonthClick = onNextMonthClick,
                            )
                            RamenLoadingIndicator(modifier = Modifier.fillMaxSize())
                        }

                    uiState.showError ->
                        Column(modifier = Modifier.fillMaxSize()) {
                            CalendarTopBar(
                                month = uiState.month,
                                hasPreviousMonthEvents = uiState.hasPreviousMonthEvents,
                                hasNextMonthEvents = uiState.hasNextMonthEvents,
                                onPreviousMonthClick = onPreviousMonthClick,
                                onNextMonthClick = onNextMonthClick,
                            )
                            LoadErrorContent(
                                image = Res.drawable.laduck_error_crying,
                                title = stringResource(Res.string.event_list_error_title),
                                description = stringResource(Res.string.event_list_error_description),
                                onRetry = onRetryClick,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                    else ->
                        CalendarGrid(
                            month = uiState.month,
                            events = uiState.events,
                            notificationDates = uiState.notificationDates,
                            hasPreviousMonthEvents = uiState.hasPreviousMonthEvents,
                            hasNextMonthEvents = uiState.hasNextMonthEvents,
                            onPreviousMonthClick = onPreviousMonthClick,
                            onNextMonthClick = onNextMonthClick,
                            onSingleEventClick = onEventClick,
                            onMultipleEventsClick = { date, events ->
                                selectedDay =
                                    CalendarDaySelection(
                                        days = uiState.eventDays,
                                        initialIndex = uiState.eventDays.indexOfFirst { it.date == date },
                                    )
                            },
                        )
                }
            }
        }
        FloatingActionButton(
            onClick = onListClick,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
            containerColor = CommonColor.Black,
            contentColor = CommonColor.White,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_list),
                contentDescription = stringResource(Res.string.event_list_open),
            )
        }
    }
    selectedDay?.let { selection ->
        CalendarEventsBottomSheet(
            days = selection.days,
            initialIndex = selection.initialIndex,
            onDismiss = { selectedDay = null },
            onEventClick = { event ->
                selectedDay = null
                onEventClick(event)
            },
        )
    }
}

@Composable
private fun CalendarTopBar(
    month: CalendarMonth,
    hasPreviousMonthEvents: Boolean,
    hasNextMonthEvents: Boolean,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_chevron_left),
            contentDescription = stringResource(Res.string.event_calendar_previous_month),
            modifier =
                Modifier
                    .size(36.dp)
                    .padding(6.dp)
                    .noRippleClickable(enabled = hasPreviousMonthEvents, onClick = onPreviousMonthClick),
            tint = if (hasPreviousMonthEvents) GrayColor.C500 else GrayColor.C200,
        )
        AppText(
            text =
                stringResource(
                    Res.string.event_calendar_month_header,
                    month.year,
                    month.monthNumber.toString().padStart(2, '0'),
                ),
            modifier = Modifier.width(84.dp),
            style = AppTextStyle.T1,
            color = GrayColor.C500,
            textAlign = TextAlign.Center,
        )
        Icon(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = stringResource(Res.string.event_calendar_next_month),
            modifier =
                Modifier
                    .size(36.dp)
                    .padding(6.dp)
                    .noRippleClickable(enabled = hasNextMonthEvents, onClick = onNextMonthClick),
            tint = if (hasNextMonthEvents) GrayColor.C500 else GrayColor.C200,
        )
    }
}

@Composable
private fun CalendarGrid(
    month: CalendarMonth,
    events: List<ShopEvent>,
    notificationDates: List<LocalDate>,
    hasPreviousMonthEvents: Boolean,
    hasNextMonthEvents: Boolean,
    onPreviousMonthClick: () -> Unit,
    onNextMonthClick: () -> Unit,
    onSingleEventClick: (ShopEvent) -> Unit,
    onMultipleEventsClick: (LocalDate, List<ShopEvent>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardShape = RoundedCornerShape(16.dp)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .border(1.dp, GrayColor.C100, cardShape)
                .clip(cardShape)
                .padding(16.dp),
    ) {
        CalendarLegend()

        Spacer(Modifier.height(8.dp))

        CalendarTopBar(
            month = month,
            hasPreviousMonthEvents = hasPreviousMonthEvents,
            hasNextMonthEvents = hasNextMonthEvents,
            onPreviousMonthClick = onPreviousMonthClick,
            onNextMonthClick = onNextMonthClick,
        )

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { dayOfWeek ->
                AppText(
                    text = weekdayLabel(dayOfWeek),
                    modifier = Modifier.weight(1f),
                    style = AppTextStyle.B3,
                    color = GrayColor.C400,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        daysWithLeadingEmptyCells(month).chunked(7).forEach { week ->
            val paddedWeek = week.take(7) + List(7 - week.size) { null }
            Row(modifier = Modifier.fillMaxWidth()) {
                paddedWeek.forEach { date ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        date?.let {
                            CalendarDayCell(
                                date = it,
                                events = events.filter { event -> event.occursOn(it) },
                                hasNotification = it in notificationDates,
                                onSingleEventClick = onSingleEventClick,
                                onMultipleEventsClick = onMultipleEventsClick,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun daysWithLeadingEmptyCells(month: CalendarMonth): List<LocalDate?> = List(month.leadingEmptyCellCount()) { null } + month.days()

@Composable
private fun weekdayLabel(dayOfWeek: DayOfWeek): String =
    stringResource(
        when (dayOfWeek) {
            DayOfWeek.SUNDAY -> Res.string.event_calendar_weekday_sunday
            DayOfWeek.MONDAY -> Res.string.event_calendar_weekday_monday
            DayOfWeek.TUESDAY -> Res.string.event_calendar_weekday_tuesday
            DayOfWeek.WEDNESDAY -> Res.string.event_calendar_weekday_wednesday
            DayOfWeek.THURSDAY -> Res.string.event_calendar_weekday_thursday
            DayOfWeek.FRIDAY -> Res.string.event_calendar_weekday_friday
            DayOfWeek.SATURDAY -> Res.string.event_calendar_weekday_saturday
        },
    )

@Preview(showBackground = true)
@Composable
private fun EventCalendarRoutePreview(
    @PreviewParameter(EventCalendarPreviewParameterProvider::class) uiState: EventCalendarUiState,
) {
    RamapTheme {
        EventCalendarScreen(
            uiState = uiState,
            onListClick = {},
            onPreviousMonthClick = {},
            onNextMonthClick = {},
            onRetryClick = {},
            onEventClick = {},
        )
    }
}
