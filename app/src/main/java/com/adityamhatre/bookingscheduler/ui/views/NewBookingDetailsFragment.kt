package com.adityamhatre.bookingscheduler.ui.views

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.adityamhatre.bookingscheduler.Application
import com.adityamhatre.bookingscheduler.MainActivity
import com.adityamhatre.bookingscheduler.R
import com.adityamhatre.bookingscheduler.adapters.BookingListAdapter
import com.adityamhatre.bookingscheduler.dtos.AdapterContainer
import com.adityamhatre.bookingscheduler.dtos.AdvancePayment
import com.adityamhatre.bookingscheduler.dtos.AppDateTime
import com.adityamhatre.bookingscheduler.dtos.ApprovedPerson
import com.adityamhatre.bookingscheduler.dtos.BookingDetails
import com.adityamhatre.bookingscheduler.dtos.PaymentType
import com.adityamhatre.bookingscheduler.enums.Accommodation
import com.adityamhatre.bookingscheduler.ui.viewmodels.NewBookingDetailsViewModel
import com.adityamhatre.bookingscheduler.utils.Utils
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId


class NewBookingDetailsFragment() : Fragment() {

    private var checkInDateTime: AppDateTime? = null
    private var checkOutDateTime: AppDateTime? = null
    private var accommodationSet: Set<Accommodation>? = null
    var editMode: Boolean = false
    private var originalBookingDetails: BookingDetails? = null
    private var adapterPosition: Int = -1
    private var adapter: BookingListAdapter? = null
    private var adapterContainer: AdapterContainer<BookingListAdapter> = AdapterContainer()

    private val viewModel: NewBookingDetailsViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.checkInDateTime = checkInDateTime ?: AppDateTime(-1, -1, -1)
        viewModel.checkOutDateTime = checkOutDateTime ?: AppDateTime(-1, -1, -1)
        viewModel.accommodationSet = accommodationSet ?: emptySet()
        if (editMode) {
            viewModel.fillValues(originalBookingDetails!!)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_booking_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookingScheduleText =
            "From: ${viewModel.checkInDateTime.toHumanFormat()}" +
                    "\nTo: ${viewModel.checkOutDateTime.toHumanFormat()}" +
                    "\nIn ${
                        Accommodation.bungalow51List(viewModel.accommodationSet)
                            .joinToString { it.readableName }
                    }"
        view.findViewById<TextView>(R.id.booking_schedule).text = bookingScheduleText


        val paymentType = view.findViewById<MaterialAutoCompleteTextView>(R.id.payment_type)
        val paymentTypeContainer = view.findViewById<TextInputLayout>(R.id.payment_type_container)
        paymentType.doOnTextChanged { text, _, _, _ ->
            viewModel.paymentType = PaymentType.fromTitleCase(text.toString())
        }
        paymentType.setAdapter(viewModel.getPaymentTypeAdapter(requireContext()))
        val paymentTypeText = if (editMode)
            PaymentType.values()
                .filter { it == originalBookingDetails?.advancePaymentInfo?.paymentType }
                .map { Utils.toTitleCase(it.name) }[0]
        else paymentType.adapter.getItem(0).toString()
        paymentType.setText(paymentTypeText, false)

        val bookingForEditText = view.findViewById<TextInputEditText>(R.id.booking_for)
        val bookingForContainer = view.findViewById<TextInputLayout>(R.id.booking_for_container)
        if (editMode) {
            bookingForEditText.setText(viewModel.bookingFor)
        }
        bookingForEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.bookingFor = text.toString()
            viewModel.validate()
        }


        val numberOfPeopleEditText = view.findViewById<TextInputEditText>(R.id.number_of_people)
        val numberOfPeopleContainer =
            view.findViewById<TextInputLayout>(R.id.number_of_people_container)
        if (editMode) {
            numberOfPeopleEditText.setText(viewModel.numberOfPeople.toString())
        }
        numberOfPeopleEditText.doOnTextChanged { text, _, _, _ ->
            if (text.toString().isNotBlank()) {
                viewModel.numberOfPeople = text.toString().toInt()
            } else {
                viewModel.numberOfPeople = -1
            }
            viewModel.validate()
        }

        val phoneNumberEditText = view.findViewById<TextInputEditText>(R.id.phone_number)
        val phoneNumberContainer =
            view.findViewById<TextInputLayout>(R.id.phone_number_container)
        if (editMode) {
            phoneNumberEditText.setText(viewModel.phoneNumber)
        }
        phoneNumberEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.phoneNumber = text.toString()
            viewModel.validate()
        }

        val advanceOrNotAdvanceRadioGroup =
            view.findViewById<RadioGroup>(R.id.advance_or_not_advance_payment)

        val advanceAmountEditText =
            view.findViewById<TextInputEditText>(R.id.advance_payment_amount)
        val advanceAmountContainer =
            view.findViewById<TextInputLayout>(R.id.advance_payment_amount_container)
        advanceAmountEditText.doOnTextChanged { text, _, _, _ ->
            if (text.toString().isNotBlank()) {
                viewModel.advancePaymentAmount = text.toString().toInt()
            } else {
                viewModel.advancePaymentAmount = -1
            }
            viewModel.validate()
        }

        advanceOrNotAdvanceRadioGroup.setOnCheckedChangeListener { _, btnId ->
            if (btnId == R.id.advance_payment) {
                advanceAmountContainer.visibility = View.VISIBLE
                paymentTypeContainer.visibility = View.VISIBLE
                viewModel.advancePaymentRequired = true
                if (advanceAmountEditText.text.toString().isNotBlank()) {
                    viewModel.advancePaymentAmount = advanceAmountEditText.text.toString().toInt()
                } else {
                    viewModel.advancePaymentAmount = -1
                }
            }
            if (btnId == R.id.not_advance_payment) {
                advanceAmountContainer.visibility = View.GONE
                paymentTypeContainer.visibility = View.GONE
                viewModel.advancePaymentRequired = false
                viewModel.advancePaymentAmount = -1
            }
            viewModel.validate()
        }

        if (editMode) {
            if (viewModel.advancePaymentRequired) {
                advanceAmountEditText.setText(viewModel.advancePaymentAmount.toString())
            } else {
                view.findViewById<RadioButton>(R.id.not_advance_payment).isChecked = true
            }
        }

        val notesEditText = view.findViewById<TextInputEditText>(R.id.notes)
        val notesEditTextContainer =
            view.findViewById<TextInputLayout>(R.id.notes_container)
        if (editMode) {
            notesEditText.setText(viewModel.notes)
        }
        notesEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.notes = text.toString()
            viewModel.validate()
        }

        val bookButton = view.findViewById<AppCompatButton>(R.id.book_button)
        bookButton.text = if (editMode) "Update" else "Book"
        viewModel.isValid().observe(viewLifecycleOwner) {
            bookButton.isEnabled = it
//            bookButton.setDisabledDrawable(
//                ContextCompat.getDrawable(
//                    requireContext(),
//                    if (it) R.drawable.book_arrow else R.drawable.invalid
//                )
//            )
        }
        bookButton.setOnClickListener {
            if (!viewModel.isValid().value!!) {
                Toast.makeText(requireContext(), "Enter all data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            (requireActivity() as MainActivity).hideKeyboard()

            val loading = view.findViewById<ProgressBar>(R.id.loading_icon)
            loading.visibility = View.VISIBLE

            var error = false
            if (viewModel.bookingFor.isBlank()) {
                bookingForContainer.error = "Provide a name"
                loading.visibility = View.GONE
                error = true
            }
            if (viewModel.phoneNumber.isBlank()) {
                phoneNumberContainer.error = "Provide a phone number"
                loading.visibility = View.GONE
                error = true
            }
            if (viewModel.numberOfPeople < 1) {
                numberOfPeopleContainer.error = "Enter number"
                loading.visibility = View.GONE
                error = true
            }
            if (viewModel.advancePaymentRequired && viewModel.advancePaymentAmount < 1) {
                advanceAmountContainer.error = "Enter amount"
                loading.visibility = View.GONE
                error = true
            }

            if (error) {
                return@setOnClickListener
            }

            val bookingDetails = BookingDetails(
                accommodations = viewModel.accommodationSet,
                checkIn = viewModel.checkInDateTime.toInstant(),
                checkOut = viewModel.checkOutDateTime.toInstant(),
                bookingMainPerson = viewModel.bookingFor,
                totalNumberOfPeople = viewModel.numberOfPeople,
                bookedBy = if (editMode) originalBookingDetails!!.bookedBy else ApprovedPerson.findByEmail(
                    Application.getInstance().account.name
                ),
                advancePaymentInfo = AdvancePayment(
                    viewModel.advancePaymentRequired,
                    viewModel.advancePaymentAmount,
                    viewModel.paymentType
                ),
                phoneNumber = viewModel.phoneNumber,
                bookingIdOnGoogle = viewModel.bookingIdOnGoogle,
                eventIds = if (editMode) originalBookingDetails!!.eventIds else ArrayList(),
                notes = viewModel.notes
            )

            Handler(requireActivity().mainLooper).postDelayed({
                try {

                } catch (ie: IllegalStateException) {
                    Log.e(
                        "NewBookingDetailsFragment:viewModel.createBooking#invokeOnCompletion",
                        "Not attached to fragment"
                    )
                }
            }, 2000)

            val returnIds = mutableListOf<Pair<String, String>>()
            viewLifecycleOwner.lifecycleScope.launch {
                if (editMode) {
                    viewModel.updateBooking(bookingDetails)
                } else {
                    returnIds.addAll(viewModel.createBooking(bookingDetails))
                }
            }.invokeOnCompletion {
                loading.visibility = View.GONE
                val bookingDetailsWithEventIds = BookingDetails(
                    accommodations = viewModel.accommodationSet,
                    checkIn = viewModel.checkInDateTime.toInstant(),
                    checkOut = viewModel.checkOutDateTime.toInstant(),
                    bookingMainPerson = viewModel.bookingFor,
                    totalNumberOfPeople = viewModel.numberOfPeople,
                    bookedBy = if (editMode) originalBookingDetails!!.bookedBy else ApprovedPerson.findByEmail(
                        Application.getInstance().account.name
                    ),
                    advancePaymentInfo = AdvancePayment(
                        viewModel.advancePaymentRequired,
                        viewModel.advancePaymentAmount,
                        viewModel.paymentType
                    ),
                    phoneNumber = viewModel.phoneNumber,
                    bookingIdOnGoogle = viewModel.bookingIdOnGoogle,
                    eventIds = if (editMode) originalBookingDetails!!.eventIds else ArrayList(returnIds),
                    notes = viewModel.notes
                )
                Toast.makeText(
                    requireContext(),
                    if (!editMode) "Added new booking" else "Updated booking",
                    Toast.LENGTH_SHORT
                ).show()
                if (editMode) {
                    adapter?.setItem(adapterPosition, bookingDetailsWithEventIds)
                } else {
                    adapterContainer.getAdapter()?.addItem(bookingDetailsWithEventIds)
                }
                if (!editMode) {
                    requireActivity().onBackPressed()
                }
                requireActivity().onBackPressed()
            }

        }

    }

    companion object {
        @JvmStatic
        fun newInstance(
            checkInDateTime: AppDateTime,
            checkOutDateTime: AppDateTime,
            accommodationSet: Set<Accommodation>,
            adapterContainer: AdapterContainer<BookingListAdapter>
        ) =
            NewBookingDetailsFragment().apply {
                this.checkInDateTime = checkInDateTime
                this.checkOutDateTime = checkOutDateTime
                this.accommodationSet = accommodationSet
                this.adapterContainer = adapterContainer
            }

        @JvmStatic
        fun newInstance(
            adapterPosition: Int,
            bookingDetails: BookingDetails,
            adapter: BookingListAdapter
        ): NewBookingDetailsFragment {
            return NewBookingDetailsFragment().apply {
                this.checkInDateTime = bookingDetails.checkIn.toAppDateTime()
                this.checkOutDateTime = bookingDetails.checkOut.toAppDateTime()
                this.accommodationSet = bookingDetails.accommodations
                this.editMode = true
                this.originalBookingDetails = bookingDetails
                this.adapterPosition = adapterPosition
                this.adapter = adapter
            }
        }
    }
}

private fun Instant.toAppDateTime(): AppDateTime {
    return with(
        this.atZone(ZoneId.of(ZoneId.SHORT_IDS["IST"]))
            .toLocalDateTime()
    ) { AppDateTime(dayOfMonth, monthValue, year, hour, minute) }
}

