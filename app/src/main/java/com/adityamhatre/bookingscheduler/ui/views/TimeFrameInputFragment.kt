package com.adityamhatre.bookingscheduler.ui.views

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.adityamhatre.bookingscheduler.R
import com.adityamhatre.bookingscheduler.adapters.BookingListAdapter
import com.adityamhatre.bookingscheduler.dtos.AdapterContainer
import com.adityamhatre.bookingscheduler.dtos.AppDateTime
import com.adityamhatre.bookingscheduler.enums.Accommodation
import com.adityamhatre.bookingscheduler.ui.viewmodels.TimeFrameInputViewModel
import com.adityamhatre.bookingscheduler.utils.TwoDigitFormatter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId


private const val DATE = "date"
private const val MONTH = "month"
private const val YEAR = "year"
private const val ONE_DAY_BOOKING = "one_day_booking"

class TimeFrameInputFragment() :
    Fragment() {
    private val viewModel: TimeFrameInputViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            with(it) {
                val date = getInt(DATE, -1)
                val month = getInt(MONTH, -1)
                val year = getInt(YEAR, -1)
                val isOneDayBooking = getBoolean(ONE_DAY_BOOKING, false)

                viewModel.checkInDateTime = AppDateTime(
                    date,
                    month,
                    year,
                    if (viewModel.checkInDateTime.hour == -1) 12 else viewModel.checkInDateTime.hour,
                    if (viewModel.checkInDateTime.minute == -1) 0 else viewModel.checkInDateTime.minute
                )
                viewModel.checkOutDateTime = AppDateTime(
                    if (viewModel.checkOutDateTime.date == -1) date else viewModel.checkOutDateTime.date,
                    if (viewModel.checkOutDateTime.month == -1) month else viewModel.checkOutDateTime.month,
                    if (viewModel.checkOutDateTime.year == -1) year else viewModel.checkOutDateTime.year,
                    if (viewModel.checkOutDateTime.hour == -1) 16 else viewModel.checkOutDateTime.hour,
                    if (viewModel.checkOutDateTime.minute == -1) 0 else viewModel.checkOutDateTime.minute
                )
                viewModel.isOneDayBooking = isOneDayBooking

            }
        }
    }

    private var pendingAdapterContainer: AdapterContainer<BookingListAdapter>? = null

    /**
     * This method should be called right after creating the fragment to pass the adapterContainer.
     * We cannot use arguments because AdapterContainer is not Serializable/Parcelable.
     */
    fun setAdapterContainer(adapterContainer: AdapterContainer<BookingListAdapter>) {
        pendingAdapterContainer = adapterContainer
        // If already attached, we can set it on the ViewModel immediately
        if (isAdded) {
            viewModel.adapterContainer = adapterContainer
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_time_frame_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pendingAdapterContainer?.let {
            viewModel.adapterContainer = it
            pendingAdapterContainer = null
        }
        viewModel.clearAccommodations()
        setupView(view)
        viewModel.alreadyChecked = false
    }


    private fun setupView(view: View) {
        setupCheckInDate(view)
        setupCheckInTime(view)
        setupCheckOutDateSpinner(view)
        setupCheckOutTime(view)
        setupCheckAvailabilityButton(view)
        setupNext(view)
        setupObservers(view)
        setupAccommodationGrid(view)
        setupOneDayBooking(view)
    }

    private fun setupOneDayBooking(view: View) {
        val oneDayBookingContainer = view.findViewById<View>(R.id.one_day_container)
        val notOneDayBookingContainer =
            view.findViewById<View>(R.id.not_one_day_container)
        val oneDayTimingRadioGroup =
            view.findViewById<RadioGroup>(R.id.one_day_booking_timing_group)

        oneDayBookingContainer.visibility =
            if (viewModel.isOneDayBooking) View.VISIBLE else View.GONE

        notOneDayBookingContainer.visibility =
            if (!viewModel.isOneDayBooking) View.VISIBLE else View.GONE

        if (viewModel.isOneDayBooking) {
            viewModel.addAccommodation(Accommodation.ONE_DAY)
        }

        oneDayTimingRadioGroup.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.one_day_booking_timing_9_30_am_to_5_00_pm -> {
                    viewModel.checkInDateTime.hour = 9
                    viewModel.checkInDateTime.minute = 30

                    viewModel.checkOutDateTime = viewModel.checkInDateTime.copy()
                    viewModel.checkOutDateTime.hour = 17
                    viewModel.checkOutDateTime.minute = 0
                }
                R.id.one_day_booking_timing_4_00_pm_to_11_55_pm -> {
                    viewModel.checkInDateTime.hour = 16
                    viewModel.checkInDateTime.minute = 0

                    viewModel.checkOutDateTime = viewModel.checkInDateTime.copy()
                    viewModel.checkOutDateTime.hour = 23
                    viewModel.checkOutDateTime.minute = 55
                }
                else -> {
                }
            }

        }
    }

    private fun setupAccommodationGrid(view: View) {
        val accommodationListLayout1 =
            view.findViewById<LinearLayout>(R.id.accommodation_list1)
        accommodationListLayout1.removeAllViews()
        val accommodationListLayout2 =
            view.findViewById<LinearLayout>(R.id.accommodation_list2)
        accommodationListLayout2.removeAllViews()
        view.findViewById<View>(R.id.results_card).visibility = View.GONE
    }

    private fun setupNext(view: View) {
        view.findViewById<Button>(R.id.next).setOnClickListener {
            if (!viewModel.isValid()) {
                Toast.makeText(requireContext(), "Some data is missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (viewModel.checkOutDateTime < viewModel.checkInDateTime) {
                Toast.makeText(
                    requireContext(),
                    "Check out cannot before check in",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            if (!viewModel.isDurationValid()) {
                Toast.makeText(
                    requireContext(),
                    "Booking duration cannot exceed 15 days",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(
                    R.id.container,
                    NewBookingDetailsFragment.newInstance(
                        viewModel.checkInDateTime,
                        viewModel.checkOutDateTime,
                        viewModel.getSelectedAccommodations().value!!,
                        adapterContainer = viewModel.adapterContainer ?: AdapterContainer()
                    )
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupObservers(view: View) {
        viewModel.getSelectedAccommodations().observe(viewLifecycleOwner, {
            view.findViewById<Button>(R.id.next).isEnabled = it.isNotEmpty()
        })
    }

    private fun setupCheckInTime(view: View) {
        val checkInTime = view.findViewById<RadioGroup>(R.id.check_in_time)

        checkInTime.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id._12_00pm) {
                viewModel.checkInDateTime.hour = 12
                viewModel.checkInDateTime.minute = 0
            }
            if (checkedId == R.id._5_30pm) {
                viewModel.checkInDateTime.hour = 17
                viewModel.checkInDateTime.minute = 30
            }
            if (viewModel.alreadyChecked) {
                checkAvailability(view)
            }
        }
    }

    private fun setupCheckAvailabilityButton(view: View) {
        val checkAvailabilityButton = view.findViewById<Button>(R.id.check_availability)
        checkAvailabilityButton.visibility =
            if (!viewModel.isOneDayBooking) View.VISIBLE else View.GONE

        checkAvailabilityButton.setOnClickListener {
            checkAvailability(
                view,
                firstTimeCheck = true
            )
        }

    }

    private fun checkAvailability(view: View, firstTimeCheck: Boolean = false) {
        if (firstTimeCheck) {
            viewModel.alreadyChecked = firstTimeCheck
            view.findViewById<DatePicker>(R.id.check_out_date_picker).isEnabled = false
        }
        val btn = view.findViewById<Button>(R.id.check_availability)
        if (viewModel.checkOutDateTime < viewModel.checkInDateTime) {
            Toast.makeText(
                requireContext(),
                "Check out cannot before check in",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (!viewModel.isDurationValid()) {
            Toast.makeText(
                requireContext(),
                "Booking duration cannot exceed 15 days",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        btn.isEnabled = false
        view.findViewById<View>(R.id.results_card).visibility = View.VISIBLE
        view.findViewById<TextView>(R.id.accommodations_header).visibility = View.GONE
        view.findViewById<ProgressBar>(R.id.loading_icon).visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val accommodationListLayout1 =
                view.findViewById<LinearLayout>(R.id.accommodation_list1)
            accommodationListLayout1.removeAllViews()
            val accommodationListLayout2 =
                view.findViewById<LinearLayout>(R.id.accommodation_list2)
            accommodationListLayout2.removeAllViews()
            viewModel.clearAccommodations()

            val availableAccommodations = viewModel.checkAvailability(
                viewModel.checkInDateTime,
                viewModel.checkOutDateTime
            ).toSet()

            view.findViewById<TextView>(R.id.accommodations_header).visibility = View.VISIBLE

            viewModel.accommodationCheckBoxIds.clear()
            Accommodation.all().forEachIndexed { i, it ->
                val checkBox = CheckBox(requireContext())
                viewModel.accommodationCheckBoxIds.add(View.generateViewId())
                checkBox.id = viewModel.accommodationCheckBoxIds.last()

                checkBox.text = it.readableName
                checkBox.isEnabled = availableAccommodations.contains(it)
                checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                    val accommodation = Accommodation.byReadableName(buttonView.text.toString())
                    if (isChecked) {
                        viewModel.addAccommodation(accommodation)
                    } else {
                        viewModel.removeAccommodation(accommodation)
                    }
                }
                if (i < Accommodation.all().size / 2) {
                    accommodationListLayout1.addView(checkBox)
                } else {
                    accommodationListLayout2.addView(checkBox)
                }
            }
            if (viewModel.isOneDayBooking) {
                viewModel.addAccommodation(Accommodation.ONE_DAY)
            }

        }.invokeOnCompletion {
            view.findViewById<ProgressBar>(R.id.loading_icon).visibility = View.GONE
            setupSelectAllButton(view)
            setupBungalow51Button(view)
            setupBungalowAndRoomsButton(view)
            btn.isEnabled = true
            val scrollView = view.findViewById<NestedScrollView>(R.id.main_scroll_view)
            scrollView.post {
                scrollView.smoothScrollTo(0, scrollView.getChildAt(0).height)
            }
        }
    }

    private fun setupBungalowAndRoomsButton(view: View) {
        val bungalowAndRoomsButton = view.findViewById<Button>(R.id.bungalow_and_rooms)
        bungalowAndRoomsButton.visibility = View.VISIBLE
        bungalowAndRoomsButton.isEnabled = viewModel.accommodationCheckBoxIds.subList(0, 10).all {
            val checkBox = view.findViewById<CheckBox>(it)
            checkBox.isEnabled
        }
        bungalowAndRoomsButton.setOnClickListener {
            viewModel.bungalowAndRoomsSelected = !viewModel.bungalowAndRoomsSelected

            viewModel.accommodationCheckBoxIds.subList(0, 10).filter {
                view.findViewById<CheckBox>(it).isEnabled
            }.map { view.findViewById<CheckBox>(it) }
                .forEach {
                    it.isChecked = viewModel.bungalowAndRoomsSelected
                }
        }
    }

    private fun setupSelectAllButton(view: View) {
        val selectAllButton = view.findViewById<Button>(R.id.select_all)
        val selectAllText = getString(R.string.select_all)
        val deselectAllText = getString(R.string.deselect_all)
        selectAllButton.visibility = View.VISIBLE
        selectAllButton.isEnabled = viewModel.accommodationCheckBoxIds.any {
            val checkBox = view.findViewById<CheckBox>(it)
            checkBox.isEnabled
        }
        selectAllButton.setOnClickListener {
            viewModel.selectWholeResort = !viewModel.selectWholeResort
            if (!viewModel.selectWholeResort) {
                viewModel.bungalow51Selected = false
                viewModel.bungalowAndRoomsSelected = false
            }
            if (viewModel.selectWholeResort) {
                selectAllButton.text = deselectAllText
            } else {
                selectAllButton.text = selectAllText
            }
            viewModel.accommodationCheckBoxIds.filter {
                view.findViewById<CheckBox>(it).isEnabled
            }.map { view.findViewById<CheckBox>(it) }
                .forEach {
                    it.isChecked = viewModel.selectWholeResort
                }
        }
    }

    private fun setupBungalow51Button(view: View) {
        val bungalowButton = view.findViewById<Button>(R.id.bungalow5_1)
        bungalowButton.visibility = View.VISIBLE
        bungalowButton.isEnabled = viewModel.accommodationCheckBoxIds.subList(0, 3).all {
            val checkBox = view.findViewById<CheckBox>(it)
            checkBox.isEnabled
        }
        bungalowButton.setOnClickListener {
            viewModel.bungalow51Selected = !viewModel.bungalow51Selected

            viewModel.accommodationCheckBoxIds.subList(0, 3).filter {
                view.findViewById<CheckBox>(it).isEnabled
            }.map { view.findViewById<CheckBox>(it) }
                .forEach {
                    it.isChecked = viewModel.bungalow51Selected
                }
        }
    }

    private fun setupCheckOutTime(view: View) {
        val checkOutTime = view.findViewById<RadioGroup>(R.id.check_out_time)

        checkOutTime.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id._11am) {
                viewModel.checkOutDateTime.hour = 11
            }
            if (checkedId == R.id._4pm) {
                viewModel.checkOutDateTime.hour = 16
            }
            if (viewModel.alreadyChecked) {
                checkAvailability(view)
            }

        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCheckOutDateSpinner(view: View) {
        val checkOutDate = view.findViewById<DatePicker>(R.id.check_out_date_picker)

        with(viewModel.checkOutDateTime) {
            checkOutDate.updateDate(year, month - 1, date)
        }
        checkOutDate.minDate = 0
        checkOutDate.minDate = LocalDate.of(
            viewModel.checkInDateTime.year,
            viewModel.checkInDateTime.month,
            viewModel.checkInDateTime.date
        ).atStartOfDay(ZoneId.systemDefault())
            .toEpochSecond() * 1000 // for milliseconds

        checkOutDate.setOnDateChangedListener { _, year, monthOfYear, dayOfMonth ->
            viewModel.checkOutDateTime.year = year
            viewModel.checkOutDateTime.month = monthOfYear + 1
            viewModel.checkOutDateTime.date = dayOfMonth
        }
    }

    private fun setupCheckInDate(view: View) {
        val checkInDate = view.findViewById<TextView>(R.id.check_in_date)
        val checkInDateText =
            "Check in on ${TwoDigitFormatter.toTwoDigits(viewModel.checkInDateTime.date)}/${
                TwoDigitFormatter.toTwoDigits(viewModel.checkInDateTime.month)
            }/${viewModel.checkInDateTime.year}\nSelect check out date and time"
        checkInDate.text = checkInDateText
    }

    companion object {
        fun newInstance(
            date: Int,
            month: Int,
            year: Int,
            adapterContainer: AdapterContainer<BookingListAdapter>,
            oneDayBooking: Boolean = false
        ) = TimeFrameInputFragment().apply {
            arguments = Bundle().apply {
                putInt(DATE, date)
                putInt(MONTH, month)
                putInt(YEAR, year)
                putBoolean(ONE_DAY_BOOKING, oneDayBooking)
            }
            // Temporarily set it, but ViewModel should ideally be initialized with it if possible
            // or it should be passed via a shared ViewModel. 
            // Since we use 'by viewModels()', we can set it here if the fragment is already attached,
            // but newInstance is called before attachment.
            // A better way is to handle this in onCreate or use a property.
        }
    }
}
