package com.rst.gadissalonmanagementsystemapp.ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.rst.gadissalonmanagementsystemapp.AdminMainActivity

import com.rst.gadissalonmanagementsystemapp.IncomeRecord
import com.rst.gadissalonmanagementsystemapp.databinding.FragmentAdminIncomeBinding
import com.rst.gadissalonmanagementsystemapp.util.NetworkUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class AdminIncomeFragment : Fragment() {

    private var _binding: FragmentAdminIncomeBinding? = null
    private val binding get() = _binding!!
    private val TAG = "AdminIncomeFragment"
    private lateinit var db: FirebaseFirestore

    // Listeners for real-time updates
    private var totalIncomeListener: ListenerRegistration? = null
    private var incomeRecordsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminIncomeBinding.inflate(inflater, container, false)
        db = Firebase.firestore
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AdminMainActivity)?.showBackButton(true)
        (activity as? AdminMainActivity)?.updateTitle("Income Report")
    }

    override fun onStart() {
        super.onStart()
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            showOfflineUI(false)
            setupIncomeStats()
        } else {
            showOfflineUI(true)
        }
    }

    override fun onStop() {
        super.onStop()
        // Detach listeners to prevent memory leaks
        totalIncomeListener?.remove()
        incomeRecordsListener?.remove()
    }

    private fun showOfflineUI(isOffline: Boolean) {
        binding.offlineLayout.root.visibility = if (isOffline) View.VISIBLE else View.GONE
        binding.contentContainer.visibility = if (isOffline) View.GONE else View.VISIBLE
    }

    private fun setupIncomeStats() {
        binding.shimmerViewContainer.startShimmer()
        binding.shimmerViewContainer.visibility = View.VISIBLE
        binding.contentContainer.visibility = View.INVISIBLE

        // 1. Get Total Income (Phase 1)
        val totalIncomeRef = db.collection("app_content").document("income_tracking")
        totalIncomeListener = totalIncomeRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed for total income.", e)
                binding.totalIncomeDisplay.text = "Error"
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val totalIncome = snapshot.getDouble("totalIncome") ?: 0.0
                binding.totalIncomeDisplay.text = "R ${"%.2f".format(totalIncome)}"
            } else {
                binding.totalIncomeDisplay.text = "R 0.00"
            }
            // Stop shimmer after first listener completes (or both)
            stopShimmer()
        }

        // 2. Calculate Today, Week, Month (Phase 1 & 2)
        val now = Calendar.getInstance()
        val startOfToday = getStartOfDay(now).time
        val startOfWeek = getStartOfWeek(now).time
        val startOfMonth = getStartOfMonth(now).time

        Log.d(TAG, "Querying records since: $startOfMonth")

        val incomeQuery = db.collection("income_records")
            .whereGreaterThanOrEqualTo("createdAt", startOfMonth)

        incomeRecordsListener = incomeQuery.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed for income records.", e)
                binding.incomeTodayDisplay.text = "Error"
                binding.incomeWeekDisplay.text = "Error"
                binding.incomeMonthDisplay.text = "Error"
                return@addSnapshotListener
            }

            var todaySum = 0.0
            var weekSum = 0.0
            var monthSum = 0.0

            if (snapshot != null) {
                for (doc in snapshot.documents) {
                    val record = doc.toObject(IncomeRecord::class.java)
                    if (record?.createdAt != null) {
                        val amount = record.amount
                        val createdAtTime = record.createdAt.time

                        // All records in this query are for this month
                        monthSum += amount

                        if (createdAtTime >= startOfWeek.time) {
                            weekSum += amount
                        }

                        if (createdAtTime >= startOfToday.time) {
                            todaySum += amount
                        }
                    }
                }
            }

            // Update UI
            binding.incomeTodayDisplay.text = "R ${"%.2f".format(todaySum)}"
            binding.incomeWeekDisplay.text = "R ${"%.2f".format(weekSum)}"
            binding.incomeMonthDisplay.text = "R ${"%.2f".format(monthSum)}"

            // Stop shimmer after second listener completes
            stopShimmer()
        }
    }

    private fun stopShimmer() {
        if (binding.shimmerViewContainer.isShimmerStarted) {
            binding.shimmerViewContainer.stopShimmer()
            binding.shimmerViewContainer.visibility = View.GONE
            binding.contentContainer.visibility = View.VISIBLE
        }
    }

    // Helper functions for date calculations
    private fun getStartOfDay(calendar: Calendar): Calendar {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    private fun getStartOfWeek(calendar: Calendar): Calendar {
        val cal = getStartOfDay(calendar) // Start from beginning of today
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek) // Go to the first day of the week (e.g., Sunday)
        return cal
    }

    private fun getStartOfMonth(calendar: Calendar): Calendar {
        val cal = getStartOfDay(calendar) // Start from beginning of today
        cal.set(Calendar.DAY_OF_MONTH, 1) // Go to the first day of the month
        return cal
    }


    override fun onDestroyView() {
        super.onDestroyView()
        // Detach listeners
        totalIncomeListener?.remove()
        incomeRecordsListener?.remove()
        _binding = null
    }
}
