package com.kratsapps.memedom.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kratsapps.memedom.R
import com.kratsapps.memedom.models.TutorialModel
import kotlinx.android.synthetic.main.image_cell.view.*
import kotlinx.android.synthetic.main.tutorial_item.view.*

class TutorialAdapter(private val tutorialList: MutableList<TutorialModel>): RecyclerView.Adapter<TutorialAdapter.TutorialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(
            R.layout.tutorial_item,
            parent,
            false
        )
        return TutorialViewHolder(v)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        val tutorialModel = tutorialList[position]
        if (tutorialModel.tutorialImage != null) {
            holder.tutorialImage.setImageResource(tutorialModel.tutorialImage!!)
        }
        holder.titleTextView.text = tutorialModel.titleText
        holder.subtitleTextView.text = tutorialModel.subtitleText
    }

    class TutorialViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val tutorialImage = itemView.tutorialImage
        val titleTextView = itemView.titleTextView
        val subtitleTextView = itemView.subtitleTextView
    }

    override fun getItemCount(): Int {
        return tutorialList.count()
    }

}