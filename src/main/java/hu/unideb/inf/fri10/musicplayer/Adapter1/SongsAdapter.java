package hu.unideb.inf.fri10.musicplayer.Adapter1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import hu.unideb.inf.fri10.musicplayer.Model.GetSongs;
import hu.unideb.inf.fri10.musicplayer.Model.Utility;
import hu.unideb.inf.fri10.musicplayer.MusicAdapter;
import hu.unideb.inf.fri10.musicplayer.R;

public class SongsAdapter  extends RecyclerView.Adapter<SongsAdapter.SongsAdapterViewHolder> {

    private int selectedPosition;
    Context context;
    List<GetSongs> arraylistSongs;
    private RecyclerItemClicklistener listener;

    public SongsAdapter(Context context, List<GetSongs> arraylistSongs, RecyclerItemClicklistener listener) {
        this.context = context;
        this.arraylistSongs = arraylistSongs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongsAdapterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

       View view = LayoutInflater.from(context).inflate(R.layout.songs_row,parent,false);
        return new SongsAdapterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongsAdapterViewHolder holder, int position) {

        GetSongs getSongs = arraylistSongs.get(position);

        if(getSongs != null){
            if(selectedPosition == position){
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context,R.color.design_default_color_primary));

            }
            else{
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context,R.color.design_default_color_secondary));

            }
            holder.iv_play_active.setVisibility(View.VISIBLE);
        }

        holder.tv_title.setText(getSongs.getSongTitle());
        holder.tv_artist.setText(getSongs.getArtist());
        String duration = Utility.convertDuration(Long.parseLong(getSongs.getSongDuration()));
        holder.tv_duration.setText(duration);
        holder.bind(getSongs,listener);


    }

    @Override
    public int getItemCount() {
        return arraylistSongs.size();
    }

    public class SongsAdapterViewHolder  extends RecyclerView.ViewHolder{


        private TextView tv_title, tv_artist,tv_duration;
        ImageView iv_play_active;

        public SongsAdapterViewHolder(@NonNull View itemView) {
            super(itemView);

            tv_title =itemView.findViewById(R.id.tv_title);
            tv_artist = itemView.findViewById(R.id.tv_artist);
            tv_duration= itemView.findViewById(R.id.tv_duration);
            iv_play_active= itemView.findViewById(R.id.iv_play_active);
        }

        public void bind(GetSongs getSongs, RecyclerItemClicklistener listener) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    listener.onClickListener(getSongs,getAdapterPosition());
                }
            });
        }
    }

    public interface RecyclerItemClicklistener {

        void onClickListener(GetSongs songs,int position);

    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
    }

}
