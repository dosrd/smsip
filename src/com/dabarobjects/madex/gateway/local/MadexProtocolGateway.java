/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dabarobjects.madex.gateway.local;

import com.dabarobjects.madex.phc.data.MadexConstants;
import com.dabarobjects.activity.plug.AbstractSpikeActivityPlug;
import com.dabarobjects.activity.plug.SpikeActivityPlugExecutorModel;
import com.dabarobjects.data.madex.MadexParserUtils;
import com.dabarobjects.data.operations.ResponseBundle;
import com.dabarobjects.data.operations.bridge.AbstractBridgeOperation;
import com.dabarobjects.data.operations.bridge.BridgeSettings;
import com.dabarobjects.data.operations.data.DefaultSaveDataOperation;
import com.dabarobjects.data.operations.data.DefaultUpdateDataOperation;
import com.dabarobjects.data.utils.CommonDataUtils;
import com.dabarobjects.data.utils.CommonRandomNoUtils;
import com.dabarobjects.madex.gateway.*;
import com.dabarobjects.madex.gateway.protocols.DefaultMadexPIMProtocolParser;
import com.dabarobjects.madex.gateway.protocols.MadexPIMProtocolParser;
import com.dabarobjects.madex.gateway.protocols.bindings.phc.*;
import com.dabarobjects.madex.phc.data.MadexInboundQueue;
import com.dabarobjects.madex.phc.data.MadexSMSEntry;
import com.dabarobjects.madex.phc.data.inbound.ListUntakenInboundOperation;
import com.dabarobjects.madex.phc.data.inbound.RemoveInboundOperation;
import com.dabarobjects.madex.phc.data.multipart.CheckForFullPartOperation;
import java.util.*;
import javax.swing.ImageIcon;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 *
 * @author dabar
 */
public class MadexProtocolGateway extends AbstractSpikeActivityPlug {

    private ApplicationContext contxt;
    private BridgeSettings settings;
    private boolean child;
    private MadexConfBean mConf;

    public MadexProtocolGateway(ApplicationContext contxt,MadexConfBean mConf ) {
        this(false,contxt,mConf);
    }

    private MadexProtocolGateway(boolean child,ApplicationContext contxt,MadexConfBean mConf ) {
        super(true, CommonRandomNoUtils.generateKey(12));
        this.child = child;
        this.mConf = mConf;
        this.contxt = contxt;
        //contxt = new FileSystemXmlApplicationContext("madex-beans.xml");
        settings = new BridgeSettings(MadexConstants.EMS_RESOURCE);


    }

    public static void main(String[] args) {
        ApplicationContext appContext = new FileSystemXmlApplicationContext("madex-beans.xml");
        System.out.println(appContext.getBean("smsspike"));

    }

    //id via 
    public void computeUpdate(SpikeActivityPlugExecutorModel paramContextTableModel,
            int row, int col) throws Exception {
        setStarted(true);
        if (child) {
            System.out.println("Spawning New Child...Opening Processors");
        } else {
            System.out.println("Opening Processors");
        }

        AbstractBridgeOperation bridge = new AbstractBridgeOperation(settings);
        List<MadexInboundQueue> inBound = (List<MadexInboundQueue>) bridge.//
                read(new ListUntakenInboundOperation(20));
        System.out.println("Found: " + child + ", " + inBound.size());
        if (inBound.isEmpty()) {
            if (child) {
                System.out.println("Killing Child..No Jobs....");
                setComplete(true);
                removeFromEventView(1);
                return;
            } else {
                System.out.println("Empty...Retrying...");
                pausePlugSignal(5);
                refreshEventPlug();
                return;
            }

        }

        AbstractBridgeOperation bridge1 = new AbstractBridgeOperation(settings);
        bridge1.setBatchMode(true);
        //List<DefaultUpdateDataOperation<MadexInboundQueue>> updates = new ArrayList<DefaultUpdateDataOperation<MadexInboundQueue>>();

        for (MadexInboundQueue madexInboundQueue : inBound) {
            madexInboundQueue.setTaken(Boolean.TRUE);
            madexInboundQueue.setProcessed(Boolean.FALSE);
            bridge1.write(new DefaultUpdateDataOperation<MadexInboundQueue>(madexInboundQueue));
            //System.out.println(madexInboundQueue);
        }
        bridge1.completeBatch();

        addNewEventView(new MadexProtocolGateway(true,contxt,mConf));

        for (MadexInboundQueue madexInboundQueue : inBound) {
            processSMSEntry(madexInboundQueue);
        }




        //load up untaken messages max 20 MadexInboundQueue
        //iterate over 20 and set taken status
        //spawn another plug to take the next 20
        //commence processing
        System.out.println("Complete Cycle...Retrying...");
        pausePlugSignal(5);
        refreshEventPlug();


    }

    private void processSMSEntry(MadexInboundQueue madexInboundQueue) {
        //String sender, String message, String entryId

        String sender = madexInboundQueue.getSender();
        String message = madexInboundQueue.getText();
        String entryId = madexInboundQueue.getId();



        AbstractBridgeOperation bridgex = new AbstractBridgeOperation(settings);

        if (authorizeActivity(sender, message, entryId)) {
            String responseCode = getActivity(bridgex, sender, message, entryId, contxt, settings);

            if (responseCode.equalsIgnoreCase("OK")) {
                //madexInboundQueue.setProcessed(Boolean.TRUE);
                boolean ok = bridgex.write(new RemoveInboundOperation(madexInboundQueue));
                if (ok) {
                    System.out.println("Removed Message: " + madexInboundQueue);
                } else {
                    System.out.println("Could Not Remove Message: " + madexInboundQueue);
                }
            } else {
                //ignore un processed
            }

        } else {
            //delete 
            boolean ok = bridgex.write(new RemoveInboundOperation(madexInboundQueue));
            if (ok) {
                System.out.println("Removed Improper SPAM Message: " + madexInboundQueue);
            } else {
                System.out.println("Could Not Remove Improper SPAM Message: " + madexInboundQueue);
            }
        }

    }

    private void registerProtocolProcessUnit(Map<String, MadexGatewayProcessingUnit> protocolMap,
            MadexGatewayProcessingUnit unit) {
        protocolMap.put(unit.getHeaderBinder(), unit);
    }

    private Map<String, MadexGatewayProcessingUnit> getGatewayProcessingUnits() {
        Map<String, MadexGatewayProcessingUnit> protocolMap = new HashMap<String, MadexGatewayProcessingUnit>();
        registerProtocolProcessUnit(protocolMap, new RegistrationConfirmationBinding());
        registerProtocolProcessUnit(protocolMap, new PHCMadexReportDataProtocol());
        registerProtocolProcessUnit(protocolMap, new PHCMadexReportDataGoeProtocol());
        registerProtocolProcessUnit(protocolMap, new UpdateNewPINMadexProtocol());
        registerProtocolProcessUnit(protocolMap, new ClusterRegistrationHandler());
        registerProtocolProcessUnit(protocolMap, new FindReportForMonthBinder());
        registerProtocolProcessUnit(protocolMap, new ClusterOfficeUpdateProtocolBinder());
        registerProtocolProcessUnit(protocolMap, new SubmitComplaintBinding());
        registerProtocolProcessUnit(protocolMap, new OfficerAccountRestoreBinding());
        registerProtocolProcessUnit(protocolMap, new OfficerRequestBindingProtocol());
        registerProtocolProcessUnit(protocolMap, new OfficerRequestBindingProtocol2());
        registerProtocolProcessUnit(protocolMap, new PHCOfficerInfoRegistrationBinder());
        return protocolMap;
    }

    private ResponseBundle processProtocol(MadexSMSEntry smsEntry, AbstractBridgeOperation dataAccess) {
        //Get Payload
        String payLoad = smsEntry.getMessage();
        //Load Protocol Parser
        MadexPIMProtocolParser mParser = new DefaultMadexPIMProtocolParser(payLoad);
        //Get protocol header
        if (mParser.isProtocolCompliant()) {
            String header = mParser.getHeader();
            //find a compliant protocol process unit to the header
            MadexGatewayProcessingUnit pUnit = getGatewayProcessingUnits().get(header);
            if (pUnit != null) {
                //do the processing
                String[] arrayData = mParser.getDataArray();
                String devicePointId = mParser.getDevicePointId();
                int arrLength = mParser.getDataArrayLength();
                String pin = mParser.getPIN();
                ResponseBundle rBundle = pUnit.processPayloadData(devicePointId, pin,
                        arrayData, arrLength, dataAccess,mConf, smsEntry);
                if (rBundle.getOperationSuccess()) {
                    smsEntry.setAssociatedDeviceId(devicePointId);
                    smsEntry.setProcessCompleted(Boolean.TRUE);
                    String completionId = CommonRandomNoUtils.generateKey(6);
                    smsEntry.setProcessCompletionId(completionId);
                    smsEntry.setProcessStatus("COMPLETED");
                    rBundle.putParameter("header", header);
                    return rBundle;
                } else {
                    smsEntry.setProcessCompleted(Boolean.FALSE);
                    smsEntry.setProcessStatus("FAILED");
                    smsEntry.setProcessingFeedback(rBundle.getRemarks());
                    rBundle.putParameter("header", header);
                    return rBundle;
                }
                //if response is good, we update the MadexEntry and notify the sender else we notify the sender of error
            }
        } else {
            smsEntry.setProcessCompleted(Boolean.FALSE);
            smsEntry.setProcessStatus("REJECTED");
            smsEntry.setProcessingFeedback("Illegal Procotol Bundle");
        }

        return new ResponseBundle(Boolean.FALSE, "", "This Packet is not protocol compliant");

    }

    public boolean authorizeActivity(String sender, String message, String entryId) {

        long senderValue = CommonDataUtils.countGSMInDump(sender);
        if (senderValue > 0) {
            return true;
        }
        //can authorize based on a specific terminal id or stored random PIN or sender
        return false;
    }

    private boolean isMADEXMultipart(String payLoad) {
        Map<String, String> parts = MadexParserUtils.stringToHashTable(payLoad);
        return parts.containsKey("id") && parts.containsKey("p") && parts.containsKey("m");
    }

    private String getMultiPartPayLoadId(String payLoad) {
        Map<String, String> parts = MadexParserUtils.stringToHashTable(payLoad);
        return parts.get("id");
    }

    private boolean isLastPart(String payLoad) {
        Map<String, String> parts = MadexParserUtils.stringToHashTable(payLoad);
        String splits = parts.get("p");
        String[] splitsPartsArr = splits.split(",");
        if (splitsPartsArr.length == 2) {
            String partsTotal = splitsPartsArr[1];
            String partsNo = splitsPartsArr[0];
            int partsTotalInt = Integer.parseInt(partsTotal);
            int partNoInt = Integer.parseInt(partsNo);
            return partNoInt == partsTotalInt;
        }
        return false;
    }

    private int getTotalParts(String payLoad) {
        Map<String, String> parts = MadexParserUtils.stringToHashTable(payLoad);
        String splits = parts.get("p");
        String[] splitsPartsArr = splits.split(",");
        if (splitsPartsArr.length == 2) {
            String partsTotal = splitsPartsArr[1];
            int partsTotalInt = Integer.parseInt(partsTotal);
            return partsTotalInt;
        }
        return -1;
    }

    private int getCurrentPart(String payLoad) {
        Map<String, String> parts = MadexParserUtils.stringToHashTable(payLoad);
        String splits = parts.get("p");
        String[] splitsPartsArr = splits.split(",");
        if (splitsPartsArr.length == 2) {
            String partsNo = splitsPartsArr[0];
            int partNoInt = Integer.parseInt(partsNo);
            return partNoInt;
        }
        return -1;
    }

    private MadexSMSEntry saveMessagePartInfo(String sender,
            String payLoadData,
            String gateId,
            String randId,
            BridgeSettings transactObj) {
        MadexSMSEntry se = new MadexSMSEntry();
        se.setEntryDate(new Date());
        se.setMessage(payLoadData);
        se.setSender(sender);
        se.setProcessStatus("PENDING");
        se.setGatewayId(gateId);
        se.setMessageId(randId);
        System.out.println(payLoadData);
        if (isMADEXMultipart(payLoadData)) {
            try {
                se.setMultipart(Boolean.TRUE);
                Map<String, String> parts = MadexParserUtils.stringToHashTable(payLoadData);
                String partsId = parts.get("id");
                se.setPartsId(partsId);
                String message = parts.get("m");
                se.setMessagePart(message);
                String splits = parts.get("p");
                String[] splitsPartsArr = splits.split(",");
                if (splitsPartsArr.length == 2) {
                    String partsTotal = splitsPartsArr[1];
                    String partsNo = splitsPartsArr[0];
                    int partsTotalInt = Integer.parseInt(partsTotal);
                    int partNoInt = Integer.parseInt(partsNo);
                    se.setPartNo(partNoInt);
                    se.setTotalParts(partsTotalInt);

                }
            } catch (Exception numberFormatException) {
                numberFormatException.printStackTrace();
                return null;
            }
        } else {
            se.setMultipart(Boolean.FALSE);
        }

        AbstractBridgeOperation ops = new AbstractBridgeOperation(transactObj);
        boolean ok = ops.write(new DefaultSaveDataOperation<MadexSMSEntry>(se));
        if (ok) {
            return se;
        }
        return null;


    }

    private String getPayloadBody(String payload) {
        Map<String, String> partsMap = MadexParserUtils.stringToHashTable(payload);
        String message = partsMap.get("m");
        return message;
    }

    public void parseProtocol2(String payload) {
        Map<String, String> partsMap = MadexParserUtils.stringToHashTable(payload);
        String splits = partsMap.get("p");
        String[] splitsPartsArr = splits.split(",");
        if (splitsPartsArr.length == 2) {
            if (splitsPartsArr[0] == splitsPartsArr[1]) {
                //it is last part and therefore we assemble all
                //Locate ID
                String id = partsMap.get("id");
            } else {
                String id = partsMap.get("id");
                String partsTotal = splitsPartsArr[1];
                String partsNo = splitsPartsArr[0];
                String message = partsMap.get("m");

            }
        }

    }

    /**
     * How MADEX Works messages come in the parts based on the transmission
     * protocol a multi part message for example will have the following format
     * id:<msgid>;p:<partNo,totalParts>;m:{<payload>}; the engine will first
     * parse the initial message and check if its a multi part message if is NOT
     * a multipart message, it is passed on to default impl, if it is the system
     * parses it and check if the final part has arrived, if not, it simply logs
     * it and ignores any form of processing, as soon as it sees the final part
     * which occurs where partNo==totalParts, it will load all the parts, join
     * the payload and pass it for processing
     *
     *
     * if multipart data will start with id:Z234;mp:01,02;msg{};
     *
     * @param id
     * @param request
     * @param transactObj
     * @return
     */
    public String getActivity(AbstractBridgeOperation ops, String sender,
            String payLoadData, String randId, ApplicationContext contxt,
            BridgeSettings transactObj) {

        if (payLoadData == null) {
            return "ERROR";
        }
        if (sender == null) {
            return "ERROR";
        }
        String gateId = "";

        MadexSMSEntry se = saveMessagePartInfo(sender, payLoadData, gateId, randId, transactObj);

        if (se == null) {
            return "ERROR";
        }
        if (isMADEXMultipart(payLoadData)) {

            String mPartId = getMultiPartPayLoadId(payLoadData);
            List<MadexSMSEntry> parts =
                    (List<MadexSMSEntry>) ops.read(new CheckForFullPartOperation(mPartId, sender));
            int totalParts = getTotalParts(payLoadData);
            int availableParts = parts.size();

            //Since parts could arrive in any other, we need to look out for complete parts
            if (totalParts == availableParts) {
                System.out.println("Joining Parts " + mPartId
                        + " Together: " + payLoadData);
                String[] partsJoinArr = new String[totalParts];
                for (MadexSMSEntry me : parts) {
                    int thisPart = getCurrentPart(me.getMessage());
                    partsJoinArr[thisPart - 1] = me.getMessagePart();

                }
                StringBuilder buf = new StringBuilder();

                for (String part : partsJoinArr) {
                    buf.append(part);
                }
                String fullPart = buf.toString();
                System.out.println("Full Part: " + mPartId
                        + ": " + fullPart);
                MadexSMSEntry sePart = new MadexSMSEntry();
                sePart.setEntryDate(new Date());
                sePart.setMessage(fullPart);
                sePart.setSender(sender);
                sePart.setProcessStatus("PENDING");
                sePart.setGatewayId(gateId);
                sePart.setMessageId(randId);

                boolean ok = ops.write(new DefaultSaveDataOperation<MadexSMSEntry>(sePart));

                return processMessage(contxt, ops, sePart, sender, randId);

            }
            int thisPart = getCurrentPart(payLoadData);
            System.out.println("Part No " + thisPart + " of  " + mPartId
                    + "Received! >>> " + payLoadData);
            return "OK";


        } else {
            return processMessage(contxt, ops, se, sender, randId);
        }



    }

    /**
     * Receives the assembled message parts for further processing
     * @param contxt
     * @param ops
     * @param se
     * @param sender
     * @param randId
     * @return 
     */
    private String processMessage(ApplicationContext contxt, AbstractBridgeOperation ops,
            MadexSMSEntry se, String sender, String randId) {
        ResponseBundle processedOk = processProtocol(se, ops);
        SpikeActivityPlugExecutorModel model = SpikeActivityPlugExecutorModel.getInstance();
        if (processedOk.getOperationSuccess()) {
            //Respond with success message

            AbstractBridgeOperation bridgex = new AbstractBridgeOperation(settings);

            MadexBackgroundSMSResponseService sPlug = new MadexBackgroundSMSResponseService(randId,
                    sender, bridgex, se, contxt, processedOk,mConf);

            model.addEventPlugAtTop(sPlug);

            //MadexBackgroundService.get().addBackgroundTask(sPlug);

            return "OK";
        } else {
            AbstractBridgeOperation bridgex = new AbstractBridgeOperation(settings);
            MadexBackgroundSMSResponseService sPlug =
                    new MadexBackgroundSMSResponseService(randId,
                    sender, bridgex, se, contxt, processedOk,mConf);

            //MadexBackgroundService.get().addBackgroundTask(sPlug);
            model.addEventPlugAtTop(sPlug);
            return "ERROR";
        }
    }

    public ImageIcon getIcon() {
        return null;
    }
}
