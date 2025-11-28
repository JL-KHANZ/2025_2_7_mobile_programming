package com.example.mobile_programming_2025_2.ui.chat.candidate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mobile_programming_2025_2.R;
import com.example.mobile_programming_2025_2.data.CandidateDTO;

import java.util.List;

/**
 * 후보자 목록(CandidateDTO)을 RecyclerView에 표시하는 Adapter.
 */
public class CandidateAdapter extends RecyclerView.Adapter<CandidateAdapter.CandidateViewHolder> {

    private List<CandidateDTO> candidateList;
    private final CandidateItemCallback callback;

    public CandidateAdapter(List<CandidateDTO> candidateList, CandidateItemCallback callback) {
        this.candidateList = candidateList;
        this.callback = callback;
    }

    // 데이터를 업데이트하는 메소드
    public void setCandidates(List<CandidateDTO> newCandidates) {
        this.candidateList = newCandidates;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CandidateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ⭐ R.layout.candidate_item을 사용합니다.
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.candidate_item, parent, false);
        return new CandidateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CandidateViewHolder holder, int position) {
        CandidateDTO candidate = candidateList.get(position);
        holder.bind(candidate);
    }

    @Override
    public int getItemCount() {
        return candidateList != null ? candidateList.size() : 0;
    }

    /**
     * ViewHolder: 리스트의 각 아이템 뷰를 관리합니다.
     */
    class CandidateViewHolder extends RecyclerView.ViewHolder {
        private final TextView displayName;
        private final TextView emotions;
        private final CardView cardView; // candidate_item.xml의 루트 뷰

        public CandidateViewHolder(@NonNull View itemView) {
            super(itemView);
            // ⭐ candidate_item.xml에 정의된 ID 사용
            displayName = itemView.findViewById(R.id.tv_display_name);
            emotions = itemView.findViewById(R.id.tv_emotions);
            cardView = itemView.findViewById(R.id.candidate_card);
        }

        public void bind(final CandidateDTO candidate) {
            displayName.setText(candidate.displayName);

            // 상위 2개 감정을 ' · ' 기호로 연결하여 표시합니다.
            List<String> topEmotions = candidate.topTwoEmotions;
            if (topEmotions != null && !topEmotions.isEmpty()) {
                String emotionText = String.join(" · ", topEmotions);
                emotions.setText(emotionText);
            } else {
                emotions.setText("감정 정보 없음");
            }

            // 클릭 리스너 설정: 아이템 클릭 시 콜백을 통해 Fragment로 전달
            cardView.setOnClickListener(v -> {
                if (callback != null) {
                    callback.onCandidateSelected(candidate);
                }
            });
        }
    }
}