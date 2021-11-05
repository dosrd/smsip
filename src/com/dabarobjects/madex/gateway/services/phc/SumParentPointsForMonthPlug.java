/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.services.phc;

import com.dabarobjects.activity.plug.AbstractSpikeActivityPlug;
import com.dabarobjects.activity.plug.SpikeActivityPlugExecutorModel;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.operations.data.DefaultUpdateDataOperation;
import com.dabarobjects.data.utils.date.MonthlyReportTime;
import com.dabarobjects.madex.data.points.ListPointsForPointOperation;
import com.dabarobjects.madex.gateway.MadexConfBean;
import com.dabarobjects.madex.phc.data.MadexConstants;
import com.dabarobjects.madex.phc.data.MADEXParameters;
import com.dabarobjects.madex.phc.data.MadexLocation;
import com.dabarobjects.madex.phc.data.MadexReport;
import com.dabarobjects.madex.phc.data.clusters.LoadStateByPointCodeOperation;
import com.dabarobjects.vts.data.points.results.LoadResultForDataElementForCodeByGroupTypeOperation;
import com.dabarobjects.vts.data.points.results.SumDataOperations;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.swing.ImageIcon;

/**
 *
 * @author dabar
 */
public class SumParentPointsForMonthPlug extends AbstractSpikeActivityPlug {

    private AbstractBridgeOperation executor;
    private MonthlyReportTime timePeriod;
    private MadexConfBean mConf;

    public SumParentPointsForMonthPlug(MonthlyReportTime timePeriod, MadexConfBean mConf) {
        super("SUM-" + timePeriod.getReportMonth() + "" + timePeriod.getReportYear());

        this.mConf = mConf;
        this.timePeriod = timePeriod;
        executor = new AbstractBridgeOperation(MadexConstants.EMS_RESOURCE);
    }

    @Override
    public void computeUpdate(SpikeActivityPlugExecutorModel paramContextTableModel, int row, int col) throws Exception {
        setStarted(true);
        System.out.println("Running Summations For The Month: " + timePeriod);

        for (int kl = 1; kl < 38; kl++) {
            int stateCode = kl;

            Boolean avaialable = (Boolean) executor.read(new LoadStateByPointCodeOperation.//
                    AnyStateAvailableByPointCodeOperation("" + stateCode));



            //We look out all the clusters
            if (avaialable) {
                Date reportingMonthDate = timePeriod.getDatePoint();

                MadexLocation stateLoc = (MadexLocation) //
                        executor.read(new LoadStateByPointCodeOperation("" + stateCode));
                System.out.println("State: " + stateLoc);
                List<MadexLocation> clusters = (List<MadexLocation>) //
                        executor.read(new ListPointsForPointOperation(stateLoc.getId()));
                for (MadexLocation clusterLoc : clusters) {
                    SumDataOperations//
                            .SumDataForClusterForDateOperation sumCLusterData //
                            = new SumDataOperations//
                            .SumDataForClusterForDateOperation//
                            (reportingMonthDate, clusterLoc.getId());
                    Long[] dataSet = (Long[]) //
                            executor.read(sumCLusterData);
                    if (dataSet != null) {
                        String[] elementsArr = MADEXParameters.DATA_TRANSMISSION_KEYS;
                        for (int i = 0; i < elementsArr.length; i++) {
                            String dataElementCode = elementsArr[i];
                            Long value = dataSet[i];
                            if (value == null) {
                                break;
                            }
                            MadexReport listRes = (MadexReport) //
                                    executor.read(new LoadResultForDataElementForCodeByGroupTypeOperation(
                                    dataElementCode, clusterLoc.getId(), reportingMonthDate));

                            listRes.setImpliedScore(value);
                            listRes.setSubmittedData(value);
                            listRes.setReportingDate(reportingMonthDate);
                            listRes.setOrdinal(i);
                            listRes.setLastUpdateTime(new Date());
                            executor.write(new DefaultUpdateDataOperation<MadexReport>(listRes));

                        }
                        //LoadResultForDataElementForCodeByGroupTypeOperation
                        // System.out.println("Running Computation Routine..." + stateCode + " = " + Arrays.asList(dataSet));
                    }

                    System.out.println("Completed Cluster: " + clusterLoc.getPointName() + "- Uplinking Now");
                    SpikeActivityPlugExecutorModel model =
                            SpikeActivityPlugExecutorModel.getInstance();
                    ReportUplinkSyncPlug rUplink = new ReportUplinkSyncPlug(executor, timePeriod, mConf.getReportSynchPath(), clusterLoc, "CLUSTER-" + clusterLoc.getId());
                    model.addEventPlugAtTop(rUplink);
                }
                System.out.println("Completed Clusters Under: " + stateLoc + ", Period: " + timePeriod.toString());

                SumDataOperations//
                        .SumDataForStateByClusterForDateOperation sumCLusterData //
                        = new SumDataOperations//
                        .SumDataForStateByClusterForDateOperation(reportingMonthDate, stateLoc.getId());
                Long[] dataSet = (Long[]) executor.read(sumCLusterData);
                if (dataSet != null) {
                    String[] elementsArr = MADEXParameters.DATA_TRANSMISSION_KEYS;
                    for (int i = 0; i < elementsArr.length; i++) {
                        Long value = dataSet[i];
                        if (value == null) {
                            break;
                        }
                        String dataElementCode = elementsArr[i];
                        MadexReport listRes = (MadexReport) executor.read(new LoadResultForDataElementForCodeByGroupTypeOperation(
                                dataElementCode, stateLoc.getId(), reportingMonthDate));

                        listRes.setImpliedScore(value);
                        listRes.setSubmittedData(value);
                        listRes.setReportingDate(reportingMonthDate);
                        listRes.setOrdinal(i);
                        listRes.setLastUpdateTime(new Date());
                        executor.write(new DefaultUpdateDataOperation<MadexReport>(listRes));

                    }
                    //LoadResultForDataElementForCodeByGroupTypeOperation
                    //System.out.println("Running Computation Routine..." + timePeriod
                    // + ", " + stateCode + " = " + Arrays.asList(dataSet));
                }

                SpikeActivityPlugExecutorModel model =
                        SpikeActivityPlugExecutorModel.getInstance();
                ReportUplinkSyncPlug rUplink = new ReportUplinkSyncPlug(executor, timePeriod, mConf.getReportSynchPath(), stateLoc, "STATE-" + stateLoc.getId());
                model.addEventPlugAtTop(rUplink);


            } else {
                System.out.println("No State Records Available for " + stateCode);
            }

        }

        String[] zones = MADEXParameters.ZONES;
        for (String zoneCode : zones) {
            Boolean avaialable = (Boolean) executor.read(new LoadStateByPointCodeOperation.//
                    AnyZoneAvailableByPointCodeOperation(zoneCode));


            //We look out all the clusters
            if (avaialable) {

                Date reportingMonthDate = timePeriod.getDatePoint();
                MadexLocation zoneLoc = (MadexLocation) //
                        executor.read(new LoadStateByPointCodeOperation.LoadZoneByPointCodeOperation(zoneCode));


                SumDataOperations//
                        .SumDataForZoneByStateForDateOperation sumzoneData //
                        = new SumDataOperations//
                        .SumDataForZoneByStateForDateOperation(reportingMonthDate, zoneLoc.getId());
                Long[] dataSet = (Long[]) executor.read(sumzoneData);
                if (dataSet != null) {
                    String[] elementsArr = MADEXParameters.DATA_TRANSMISSION_KEYS;
                    for (int i = 0; i < elementsArr.length; i++) {
                        Long value = dataSet[i];
                        if (value == null) {
                            break;
                        }
                        String dataElementCode = elementsArr[i];
                        MadexReport listRes = (MadexReport) executor.read(new LoadResultForDataElementForCodeByGroupTypeOperation(
                                dataElementCode, zoneLoc.getId(), reportingMonthDate));

                        listRes.setImpliedScore(value);
                        listRes.setSubmittedData(value);
                        listRes.setReportingDate(reportingMonthDate);
                        listRes.setOrdinal(i);
                        listRes.setLastUpdateTime(new Date());
                        executor.write(new DefaultUpdateDataOperation<MadexReport>(listRes));

                    }
                    //LoadResultForDataElementForCodeByGroupTypeOperation
                    //System.out.println("Running Computation Routine..." +rMonth
                    //+ ", " + zoneLoc.getPointName() + " = " + Arrays.asList(dataSet));
                    SpikeActivityPlugExecutorModel model =
                            SpikeActivityPlugExecutorModel.getInstance();
                    ReportUplinkSyncPlug rUplink = new ReportUplinkSyncPlug(executor, timePeriod, mConf.getReportSynchPath(), zoneLoc, "ZONE-" + zoneLoc.getId());
                    model.addEventPlugAtTop(rUplink);
                }



            }

        }

        Boolean avaialable = (Boolean) executor.read(new LoadStateByPointCodeOperation.//
                AnyNationAvailableByPointCodeOperation());


        //We look out all the clusters
        if (avaialable) {

            Date reportingMonthDate = timePeriod.getDatePoint();
            MadexLocation nationLoc = (MadexLocation) //
                    executor.read(new LoadStateByPointCodeOperation.LoadTopLocationOperation());

            SumDataOperations//
                    .SumDataForNationByStateForDateOperation sumCLusterData //
                    = new SumDataOperations//
                    .SumDataForNationByStateForDateOperation(reportingMonthDate, nationLoc.getId());
            Long[] dataSet = (Long[]) executor.read(sumCLusterData);
            if (dataSet != null) {
                String[] elementsArr = MADEXParameters.DATA_TRANSMISSION_KEYS;
                for (int i = 0; i < elementsArr.length; i++) {
                    Long value = dataSet[i];
                    if (value == null) {
                        break;
                    }
                    String dataElementCode = elementsArr[i];
                    MadexReport listRes = (MadexReport) executor.read(new LoadResultForDataElementForCodeByGroupTypeOperation(
                            dataElementCode, nationLoc.getId(), reportingMonthDate));

                    listRes.setImpliedScore(value);
                    listRes.setSubmittedData(value);
                    listRes.setReportingDate(reportingMonthDate);
                    listRes.setOrdinal(i);
                    listRes.setLastUpdateTime(new Date());
                    executor.write(new DefaultUpdateDataOperation<MadexReport>(listRes));

                }
                //LoadResultForDataElementForCodeByGroupTypeOperation
                System.out.println("Running Computation Routine..." + timePeriod.toString()
                        + " NATION = " + Arrays.asList(dataSet));
                SpikeActivityPlugExecutorModel model =
                        SpikeActivityPlugExecutorModel.getInstance();
                ReportUplinkSyncPlug rUplink = new ReportUplinkSyncPlug(executor, timePeriod, mConf.getReportSynchPath(), nationLoc, "NATION");
                model.addEventPlugAtTop(rUplink);
            }

        }



        //This is last month
        //lastMonthPeriod
        //while this process runs, 
        //Date today = new Date();//if today is 3 months ahead of timePeriod, stop

        // pausePlugSignal(40);
        // refreshEventPlug();
        setComplete(true);
        removeFromEventView(1);




    }

    @Override
    public ImageIcon getIcon() {
        return null;
    }
}
