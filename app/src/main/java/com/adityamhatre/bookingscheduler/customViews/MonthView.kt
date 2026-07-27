package com.adityamhatre.bookingscheduler.customViews

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.get
import com.adityamhatre.bookingscheduler.R
import java.util.*


class MonthView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    fun interface DateClickedListener {
        fun onDateClicked(date: Int, month: Int, year: Int)
    }

    fun interface MonthClickedListener {
        fun onMonthClicked(month: Int, year: Int)
    }


    var dateClickedListener: DateClickedListener? = null
    var monthClickedListener: MonthClickedListener? = null

    private val view by lazy { inflate(context, R.layout.month_view, this) }

    private var month: Int = 1
    private var year: Int = 1970
    private var count: Int = 0
    private var titleText = ""

    private lateinit var calendar: Calendar
    private val datesLookup = mutableMapOf<Int, Pair<Int, Int>>()
    private val weekIds = intArrayOf(
        R.id.week1,
        R.id.week2,
        R.id.week3,
        R.id.week4,
        R.id.week5,
        R.id.week6
    )

    constructor(context: Context, month: Int, year: Int) : this(context, null) {
        this.month = month
        this.year = year
        this.titleText = "${getMonthName()} $year"
        fillWeeks()
    }

    init {
        if (attrs != null) {
            context.theme.obtainStyledAttributes(attrs, R.styleable.MonthView, 0, 0)
                .apply {
                    try {
                        month = getString(R.styleable.MonthView_month)?.toInt() ?: 1
                        year = getInt(R.styleable.MonthView_year, 1970)

                        titleText = "${getMonthName()} $year"

                        fillWeeks()
                    } finally {
                        recycle()
                    }
                }
        }
    }

    private fun fillWeeks() {


        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)

        calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, month - 1)
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.DAY_OF_MONTH, 1)


        var currentDate = 1
        val firstDayOfMonthIndex =
            calendar.get(Calendar.DAY_OF_WEEK) - 1//method returns 1 - 7, hence -1


        weekIds.forEachIndexed { i, weekId ->
            val weekLayout = view.findViewById<LinearLayout>(weekId)!!
            var weekFilled = false

            var dateIndex = if (i == 0) firstDayOfMonthIndex else 0
            while (dateIndex < 7 && currentDate <= maxDaysInThisMonth(month, year)) {
                weekFilled = true
                datesLookup[currentDate] = Pair(i, dateIndex)

                val dateTextView = weekLayout[dateIndex++] as TextView
                dateTextView.text = "${currentDate++}"


                dateTextView.setBackgroundResource(outValue.resourceId)
                dateTextView.setOnClickListener {
                    if (it == null) return@setOnClickListener

                    val iDateTextView = it as TextView
                    if (iDateTextView.text.trim().isEmpty()) return@setOnClickListener

                    dateClickedListener?.onDateClicked(
                        iDateTextView.text.toString().toInt(),
                        month,
                        year
                    )
                }
            }

            if (!weekFilled) {
                weekLayout.visibility = View.GONE
            }

        }
    }


    private fun getMonthName(): String {
        return when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> ""
        }
    }

    fun setBookingsCount(count: Int) {
        this.count = count
        addBookingInfo()
    }

    fun addBookingInfo() {
        val titleView = view.findViewById<TextView>(R.id.title)
        val updatedTitle = if (this.count > 0)
            "$titleText ($count booking${if (count > 1) "s" else ""})"
        else
            "$titleText (No Bookings)"
        titleView.text = updatedTitle
    }

    fun getMonthYear(): Pair<Int, Int> = Pair(month, year)

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        view.findViewById<CardView>(R.id.month_card).setOnClickListener {
            monthClickedListener?.onMonthClicked(month, year)
        }
    }

    companion object {
        fun maxDaysInThisMonth(month: Int, year: Int): Int {
            val leap = if (year % 4 == 0) {
                if (year % 100 == 0) {
                    year % 400 == 0
                } else true
            } else false

            return if (month == 2) if (leap) 29 else 28
            else if (month in intArrayOf(1, 3, 5, 7, 8, 10, 12)) 31 else 30
        }

        fun monthName(month: Int): String {
            return when (month) {
                1 -> "January"
                2 -> "February"
                3 -> "March"
                4 -> "April"
                5 -> "May"
                6 -> "June"
                7 -> "July"
                8 -> "August"
                9 -> "September"
                10 -> "October"
                11 -> "November"
                12 -> "December"
                else -> ""
            }
        }

    }
}
