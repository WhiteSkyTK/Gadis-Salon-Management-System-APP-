package com.rst.gadissalonmanagementsystemapp.ui.worker

import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import android.graphics.Color

// This class is responsible for adding a dot to dates that have events.
class EventDayDecorator(private val dates: HashSet<CalendarDay>) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        // Returns true if the set of dates contains the date to be decorated.
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        // This is what adds the dot. We can customize the color here if needed.
        view.addSpan(DotSpan(8f, Color.parseColor("#9B51E0"))) // Using your colorPrimary2 hex value
    }
}