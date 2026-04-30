package com.adityamhatre.bookingscheduler.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.adityamhatre.bookingscheduler.R
import com.adityamhatre.bookingscheduler.customViews.MonthView
import com.adityamhatre.bookingscheduler.dtos.AppDate
import com.adityamhatre.bookingscheduler.ui.viewmodels.ListOfBookingsViewModel
import com.adityamhatre.bookingscheduler.utils.TwoDigitFormatter
import com.nambimobile.widgets.efab.ExpandableFabLayout
import com.nambimobile.widgets.efab.FabOption
import kotlinx.coroutines.launch

private const val DATE = "date"
private const val MONTH = "month"
private const val YEAR = "year"

class ListOfBookingsFragment : Fragment() {

    private val viewModel: ListOfBookingsViewModel by viewModels()
    private val consentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // After user grants consent, clear cached list so VM re-fetches
            viewModel.bookingsList = null
            setupRecyclerView(requireView())
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            with(it) {
                val date = getInt(DATE, -1)
                val month = getInt(MONTH, -1)
                val year = getInt(YEAR, -1)
                viewModel.bookingsOn = AppDate(date, month, year)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_list_of_bookings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (viewModel.bookingsOn.nothingInitialized()) {
            return
        }
        viewModel.setBookingsCount(-1)
        setupTitle(view)
        setupRecyclerView(view)
        setupFab(view)
        setupObservers(view)
    }

    private fun setupObservers(view: View) {
        viewModel.getBookingsCount().observe(viewLifecycleOwner) {
            if (it == -1) {
                return@observe
            }
            val titleView = view.findViewById<TextView>(R.id.bookings_on)
            if (it != 0) {
                view.findViewById<TextView>(R.id.emptyView).visibility = View.GONE
                titleView.visibility = View.VISIBLE

                titleView.text = titleView.text.toString()
                    .replace("Loading bookings", "$it bookings")
                    .replace(Regex("\\d+\\sbooking(s+)"), "$it bookings")
                    .replace(
                        "bookings",
                        if (it == 1) "booking" else "bookings"
                    )
                    .replace("...", "")
            } else {
                view.findViewById<TextView>(R.id.emptyView).visibility = View.VISIBLE
                titleView.visibility = View.GONE
            }
        }

        viewModel.getNeedsConsentIntent().observe(viewLifecycleOwner) {
            if (it == null) {
                return@observe
            }
            view.findViewById<ProgressBar>(R.id.loading_icon).visibility = View.GONE
            consentLauncher.launch(it)
        }
    }

    private fun setupTitle(view: View) {
        val noBookingsView = view.findViewById<TextView>(R.id.emptyView)
        var dateString = ""
        var noBookingString = noBookingsView.text.toString()

        if (viewModel.bookingsOn.isForMonth()) {
            dateString =
                MonthView.monthName(viewModel.bookingsOn.month) + " " + viewModel.bookingsOn.year

            noBookingString = "No bookings for $dateString"
        } else if (viewModel.bookingsOn.isForDate()) {
            dateString = "${TwoDigitFormatter.toTwoDigits(viewModel.bookingsOn.date)}/${
                TwoDigitFormatter.toTwoDigits(viewModel.bookingsOn.month)
            }/${viewModel.bookingsOn.year}"

            noBookingString = noBookingString.replace("selected date", dateString)
        }

        val title = "Loading bookings for $dateString..."
        view.findViewById<TextView>(R.id.bookings_on).text = title

        noBookingsView.text = noBookingString
    }

    private fun setupFab(view: View) {
        val newBooking = view.findViewById<FabOption>(R.id.new_booking)
        val oneDayBooking = view.findViewById<FabOption>(R.id.one_day_booking)
        val viewBookingsInMonth = view.findViewById<FabOption>(R.id.view_all_bookings_in_month)

        if (viewModel.bookingsOn.isForMonth()) {
            view.findViewById<ExpandableFabLayout>(R.id.fab_layout).visibility = View.GONE
            newBooking.fabOptionEnabled = false
            return
        }


        newBooking.setOnClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.container,
                    TimeFrameInputFragment.newInstance(
                        viewModel.bookingsOn.date,
                        viewModel.bookingsOn.month,
                        year = viewModel.bookingsOn.year,
                        adapterContainer = viewModel.adapterContainer,
                    ).apply {
                        setAdapterContainer(viewModel.adapterContainer)
                    }
                )
                .addToBackStack(null)
                .commit()
        }

        oneDayBooking.setOnClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.container,
                    TimeFrameInputFragment.newInstance(
                        viewModel.bookingsOn.date,
                        viewModel.bookingsOn.month,
                        year = viewModel.bookingsOn.year,
                        adapterContainer = viewModel.adapterContainer,
                        oneDayBooking = true
                    ).apply {
                        setAdapterContainer(viewModel.adapterContainer)
                    }
                )
                .addToBackStack(null)
                .commit()
        }

        viewBookingsInMonth.setOnClickListener {
            requireActivity().supportFragmentManager
                .beginTransaction()
                .remove(this)
                .commit()
            requireActivity().supportFragmentManager.popBackStack()

            requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(
                    R.id.container,
                    newInstance(
                        month = viewModel.bookingsOn.month,
                        year = viewModel.bookingsOn.year
                    )
                )
                .addToBackStack(null)
                .commit()
        }
    }


    private fun setupRecyclerView(view: View) {
        val bookingRecyclerView = view.findViewById<RecyclerView>(R.id.booking_list)
        viewLifecycleOwner.lifecycleScope.launch {
            view.findViewById<ProgressBar>(R.id.loading_icon).visibility = View.VISIBLE
            bookingRecyclerView.adapter =
                viewModel.getBookingListAdapter(
                    afterUpdateComplete = { position, bookingDetailsList, adapter ->
                        requireActivity().supportFragmentManager
                            .beginTransaction()
                            .replace(
                                R.id.container,
                                NewBookingDetailsFragment.newInstance(
                                    position,
                                    bookingDetailsList,
                                    adapter
                                )
                            )
                            .addToBackStack(null)
                            .commit()
                    },
                    confirmDelete = { position, onConfirm ->
                        val builder = AlertDialog.Builder(requireContext())
                        builder.setMessage("Are you sure you want to delete?")
                            .setCancelable(false)
                            .setPositiveButton("Yes") { _, _ ->
                                viewLifecycleOwner.lifecycleScope.launch { onConfirm() }
                                bookingRecyclerView.adapter?.notifyItemRemoved(position)
                            }
                            .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                        val alert = builder.create()
                        alert.show()
                    },
                    onClick = { bookingDetails ->
                        requireActivity().supportFragmentManager
                            .beginTransaction()
                            .replace(
                                R.id.container,
                                ViewBookingDetails.newInstance(bookingDetails)
                            )
                            .addToBackStack(null)
                            .commit()
                    }
                )
        }.invokeOnCompletion {
            bookingRecyclerView.postDelayed({
                bookingRecyclerView.smoothScrollToPosition(
                    bookingRecyclerView.adapter?.itemCount ?: 0
                )
            }, 250)

            view.findViewById<ProgressBar>(R.id.loading_icon).visibility = View.GONE


        }

    }

    companion object {
        @JvmStatic
        fun newInstance(date: Int = -1, month: Int, year: Int) =
            ListOfBookingsFragment().apply {
                arguments = Bundle().apply {
                    putInt(DATE, date)
                    putInt(MONTH, month)
                    putInt(YEAR, year)
                }
            }

    }
}