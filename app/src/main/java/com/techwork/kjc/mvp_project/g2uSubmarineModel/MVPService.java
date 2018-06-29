package com.techwork.kjc.mvp_project.g2uSubmarineModel;

import com.google.firebase.database.DatabaseError;
import com.techwork.kjc.mvp_project.g2uSubmarineModel.beanse.FocusBean;
import com.techwork.kjc.mvp_project.g2uSubmarineModel.beanse.MVP_RecordAccBean;
import com.techwork.kjc.mvp_project.g2uSubmarineModel.beanse.MeasureBean;
import com.techwork.kjc.mvp_project.g2uSubmarineModel.beanse.RecursiveBean;
import com.techwork.kjc.mvp_project.g2uSubmarineModel.beanse.UserPublicInfoBean;
import com.techwork.kjc.mvp_project.g2uSubmarineModel.beanse.VesusBean;
import com.techwork.kjc.mvp_project.util.DateKey;
import com.techwork.kjc.mvp_project.util.EventChain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MVPService {

    public static void selsetMVP_Record(String uid,OnCompleteMVP_Record onCompleteMVP_record){

        EventChain eventChain = new EventChain(); // 이벤트 체인 준비

        // mesure 데이터 일별로 최고 값 합 준비
        eventChain.ready("M 준비");
        Map<DateKey, Double> M = new HashMap<>();
        MeasureDAO.selectMeasureBeanseByUID(uid, new MeasureDAO.OnSelectedMeasureBeans() {
            @Override
            public void onSelectedMeasureBeans(boolean success, List<MeasureBean> measureBeans, DatabaseError databaseError) {
                if(!success) {eventChain.complete("M 준비"); return;}
                Map<DateKey, Double[]> Meach = new HashMap<>();
                for(MeasureBean bean : measureBeans){
                    DateKey dateKey = new DateKey(bean.timestamp);
                    if(!Meach.containsKey(dateKey))
                        Meach.put(dateKey, new Double[]{0.0, 0.0, 0.0, 0.0});
                    Double[] o = Meach.get(dateKey);
                    o[0] = o[0] > bean.arm ? o[0] : bean.arm;
                    o[1] = o[1] > bean.leg ? o[1] : bean.leg;
                    o[2] = o[2] > bean.back ? o[2] : bean.back;
                    o[3] = o[3] > bean.body ? o[3] : bean.body;
                }

                for( DateKey dateKey : Meach.keySet()){
                    Double[] o = Meach.get(dateKey);
                    M.put(dateKey, o[0] + o[1] + o[2] + o[3]);
                }
                eventChain.complete("M 준비");
            }
        });

        // versus 데이터 일별로 누적 값 준비
        eventChain.ready("V 준비");
        Map<DateKey, Integer[]> V = new HashMap<>();
        VersusDAO.selectVersusBeans(uid, new VersusDAO.OnSelectedBersusBeans() {
            @Override
            public void OnSelectedBersusBeans(List<VesusBean> vesusBeans) {

                for(VesusBean bean : vesusBeans){
                    if(!bean.winner.equals(uid)) continue;
                    DateKey dateKey = new DateKey(bean.timestamp);
                    if(!V.containsKey(dateKey))
                        V.put(dateKey,new Integer[]{0});
                    Integer[] o = V.get(dateKey);
                    o[0] += 1;
                    V.put(dateKey, o);
                }
                eventChain.complete("V 준비");
            }
        });

        // Foucs 데이터 일별로 누적값 준비
        eventChain.ready("P Part1 준비");
        Map<DateKey, Long[]> P1 = new HashMap<>();
        FocusDAO.selectAllFocusBeans(uid, new FocusDAO.OnSelectedFocusBeans() {
            @Override
            public void OnSelectedFocusBeans(boolean success, List<FocusBean> focusBeans, DatabaseError databaseError) {
                if(!success) {eventChain.complete("P Part1 준비"); return;}

                for(FocusBean bean : focusBeans){
                    DateKey dateKey = new DateKey(bean.timestamp);
                    if(!P1.containsKey(dateKey))
                        P1.put(dateKey, new Long[]{0l});
                    Long[] o = P1.get(dateKey);
                    o[0] += bean.reps;
                    P1.put(dateKey, o);
                }

                eventChain.complete("P Part1 준비");
            }
        });

        // Recursive 데이터 일별로 누적값 준비
        eventChain.ready("P Part2 준비");
        Map<DateKey, Long[]> P2 = new HashMap<>();
        RecursiveDAO.selectRecursiveMap(uid, new RecursiveDAO.OnSelectedRecursiveMap() {
            @Override
            public void OnSelectedRecursiveMap(boolean success, Map<Long, RecursiveBean> recursiveBeanMap, DatabaseError databaseError) {
                if(!success) {eventChain.complete("P Part2 준비"); return;}

                for(RecursiveBean bean : recursiveBeanMap.values()){
                    DateKey dateKey = new DateKey(bean.timestamp);
                    if(!P2.containsKey(dateKey))
                        P2.put(dateKey, new Long[]{0l});
                    Long[] o = P2.get(dateKey);
                    o[0] += bean.reps;
                    P2.put(dateKey, o);
                }

                eventChain.complete("P Part2 준비");
            }
        });

        eventChain.andthen(()->{ // 개별적인 위치에서 가져온 데이터를 병합합니다.
            Map<DateKey, MVP_RecordAccBean> mvpRecordAccBeanMap = new HashMap<>();

            for(DateKey dateKey : M.keySet()){ // Mesuare 병합
                if(!mvpRecordAccBeanMap.containsKey(dateKey))
                    mvpRecordAccBeanMap.put(dateKey, new MVP_RecordAccBean(0,0,0));
                MVP_RecordAccBean o = mvpRecordAccBeanMap.get(dateKey);
                o.mVal = M.get(dateKey);
                mvpRecordAccBeanMap.put(dateKey,o);
            }
            for(DateKey dateKey : V.keySet()){
                if(!mvpRecordAccBeanMap.containsKey(dateKey))
                    mvpRecordAccBeanMap.put(dateKey, new MVP_RecordAccBean(0,0,0));
                MVP_RecordAccBean o = mvpRecordAccBeanMap.get(dateKey);
                o.vVal = V.get(dateKey)[0];
                mvpRecordAccBeanMap.put(dateKey,o);
            }
            for(DateKey dateKey : P1.keySet()){
                if(!mvpRecordAccBeanMap.containsKey(dateKey))
                    mvpRecordAccBeanMap.put(dateKey, new MVP_RecordAccBean(0,0,0));
                MVP_RecordAccBean o = mvpRecordAccBeanMap.get(dateKey);
                o.pVal += P1.get(dateKey)[0];
                mvpRecordAccBeanMap.put(dateKey,o);
            }
            for(DateKey dateKey : P2.keySet()){
                if(!mvpRecordAccBeanMap.containsKey(dateKey))
                    mvpRecordAccBeanMap.put(dateKey, new MVP_RecordAccBean(0,0,0));
                MVP_RecordAccBean o = mvpRecordAccBeanMap.get(dateKey);
                o.pVal += P2.get(dateKey)[0];
                mvpRecordAccBeanMap.put(dateKey,o);
            }

            onCompleteMVP_record.onCompleteMVP_Record(mvpRecordAccBeanMap);
        }, "M 준비", "V 준비", "P Part1 준비", "P Part2 준비");
    }
    public interface OnCompleteMVP_Record{
        void onCompleteMVP_Record(Map<DateKey, MVP_RecordAccBean> mvpRecordAccBeanMap);
    }
}
