package com.knowway.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.paging.PagingDataAdapter
import com.knowway.R
import com.knowway.data.model.department.DepartmentStoreResponse

class AdminDepartmentStoreAdapter(
        private val onItemClicked: (DepartmentStoreResponse) -> Unit
) : PagingDataAdapter<DepartmentStoreResponse, AdminDepartmentStoreAdapter.DepartmentStoreViewHolder>(DepartmentStoreComparator) {

        private var selectedPosition: Int? = null
        private var searchResults: List<DepartmentStoreResponse> = emptyList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepartmentStoreViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_department, parent, false)
                return DepartmentStoreViewHolder(view, onItemClicked)
        }

        override fun onBindViewHolder(holder: DepartmentStoreViewHolder, position: Int) {
                val departmentStore = if (searchResults.isNotEmpty()) {
                        searchResults[position]
                } else {
                        getItem(position)
                }
                if (departmentStore != null) {
                        holder.bind(departmentStore, position)
                }
        }

        override fun getItemCount(): Int {
                return if (searchResults.isNotEmpty()) {
                        searchResults.size
                } else {
                        super.getItemCount()
                }
        }

        inner class DepartmentStoreViewHolder(
                itemView: View,
                private val onItemClicked: (DepartmentStoreResponse) -> Unit
        ) : RecyclerView.ViewHolder(itemView) {
                private val deptNameTextView: TextView = itemView.findViewById(R.id.dept_name)
                private val deptBranchTextView: TextView = itemView.findViewById(R.id.dept_branch)
                private val checkIconImageView: ImageView = itemView.findViewById(R.id.check_icon)

                fun bind(departmentStoreResponse: DepartmentStoreResponse, position: Int) {
                        deptNameTextView.text = departmentStoreResponse.departmentStoreName
                        deptBranchTextView.text = departmentStoreResponse.departmentStoreBranch
                        updateCheckIcon(position)

                        itemView.setOnClickListener {
                                if (selectedPosition == position) {
                                        selectedPosition = null
                                } else {
                                        selectedPosition = position
                                }
                                notifyDataSetChanged()
                                onItemClicked(departmentStoreResponse)
                        }
                }

                private fun updateCheckIcon(position: Int) {
                        checkIconImageView.setImageResource(if (selectedPosition == position) R.drawable.ic_blue_check else R.drawable.ic_black_check)
                }
        }

        object DepartmentStoreComparator : DiffUtil.ItemCallback<DepartmentStoreResponse>() {
                override fun areItemsTheSame(oldItem: DepartmentStoreResponse, newItem: DepartmentStoreResponse): Boolean {
                        return oldItem.departmentStoreId == newItem.departmentStoreId
                }

                override fun areContentsTheSame(oldItem: DepartmentStoreResponse, newItem: DepartmentStoreResponse): Boolean {
                        return oldItem == newItem
                }
        }

        fun getSelectedDepartmentStore(): DepartmentStoreResponse? {
                return selectedPosition?.let {
                        if (searchResults.isNotEmpty()) searchResults[it] else getItem(it)
                }
        }

        fun submitSearchResults(results: List<DepartmentStoreResponse>) {
                searchResults = results
                notifyDataSetChanged()
        }

        fun clearSearchResults() {
                searchResults = emptyList()
                notifyDataSetChanged()
        }
}
