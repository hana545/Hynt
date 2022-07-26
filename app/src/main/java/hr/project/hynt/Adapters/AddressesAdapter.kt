package hr.project.hynt.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import hr.project.hynt.FirebaseDatabase.Address
import hr.project.hynt.FirebaseDatabase.Review
import hr.project.hynt.R
import java.util.*
import java.util.Collections.emptyList

class AddressesAdapter(private val mList: List<Address>, private val mList_id: List<String>? = emptyList(), val mItemClickListener: ItemClickListener) : RecyclerView.Adapter<AddressesAdapter.ViewHolder>() {


    val db = Firebase.database("https://hynt-cb624-default-rtdb.europe-west1.firebasedatabase.app")
    val authUser = FirebaseAuth.getInstance().currentUser

    interface ItemClickListener{
        fun onItemClick(address : Address, addressId : String)
        fun onBtnDelete(address : Address, addressId : String)
    }

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        var view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_my_address_card, parent, false)


        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val address = mList[position]

        holder.card!!.setOnClickListener{
            mItemClickListener.onItemClick(address, mList_id!!.get(position))
        }
        holder.btnDeleteAddress!!.setOnClickListener{
            mItemClickListener.onBtnDelete(address, mList_id!!.get(position))
        }
        holder.addressName.text = address.name
        if (address.category == "Home"){
            holder.addressCategory.setImageResource(R.drawable.ic_address_category_home)
        } else if (address.category == "Work"){
            holder.addressCategory.setImageResource(R.drawable.ic_address_category_work)
        } else if (address.category == "Education"){
            holder.addressCategory.setImageResource(R.drawable.ic_address_category_education)
        } else {
            holder.addressCategory.setImageResource(R.drawable.ic_address_category_other)
        }




    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val card : CardView? = itemView.findViewById(R.id.address_card)
        val addressName: TextView = itemView.findViewById(R.id.address_name)
        val addressCategory: ImageView = itemView.findViewById(R.id.address_category)
        val btnDeleteAddress: ImageButton = itemView.findViewById(R.id.btn_delete_address)

    }


}