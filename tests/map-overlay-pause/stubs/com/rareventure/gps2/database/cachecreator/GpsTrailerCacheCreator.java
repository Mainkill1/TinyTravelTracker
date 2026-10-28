package com.rareventure.gps2.database.cachecreator;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import com.rareventure.android.SuperThreadManager;
import com.rareventure.util.ReadWriteThreadManager;
import com.rareventure.gps2.GTG;
import com.rareventure.gps2.database.cache.AreaPanelSpaceTimeBox;
import com.rareventure.gps2.reviewer.map.ViewLine;
public class GpsTrailerCacheCreator {
    public static final int CALC_VIEW_NODES_STILL_MORE_NODES_TO_CALC = 1;
    public static final int CALC_VIEW_NODES_LINES_NEED_RECALC = 2;
    public final ReadWriteThreadManager viewNodeThreadManager = new ReadWriteThreadManager();
    public final HashMap<Integer, ViewLine> startTimeToViewLine = new HashMap<>();
    public final HashMap<Integer, ViewLine> endTimeToViewLine = new HashMap<>();
    public final CountDownLatch paused = new CountDownLatch(1);
    public SuperThreadManager manager;
    public ViewNode head = ViewNode.createHeadViewNode();
    public volatile int calls;
    public int pauseOnCall = 1;
    public int totalCalls = 20;
    public boolean delayPass;
    public boolean shutdownOnFirstPass;
    public ViewNode partialHead;
    public int calcViewableNodes(AreaPanelSpaceTimeBox box, int depth, int earliest, int latest) {
        if (!GTG.cacheCreatorLock.isReadingThreadsActive())
            throw new AssertionError("overlay calculation requires cache read lock");
        viewNodeThreadManager.registerWritingThread();
        try {
            head.status = ViewNode.VNStatus.SET;
            head.stBox = box;
            head.overlappingRange = new int[] {
                Math.max(box.minZ, GTG.apCache.top.recordedStart),
                Math.min(box.maxZ, GTG.apCache.top.recordedEnd)
            };
            head.dirtyDescendents = calls + 1 < totalCalls ? 1 : 0;
            if (calls == 0) partialHead = head;
            if (shutdownOnFirstPass && calls == 0) manager.isShutdown = true;
            if (++calls == pauseOnCall) {
                manager.pauseAllSuperThreads();
                paused.countDown();
            }
            if (delayPass) {
                try { Thread.sleep(60); }
                catch (InterruptedException e) { throw new AssertionError(e); }
            }
            return calls < totalCalls ? CALC_VIEW_NODES_STILL_MORE_NODES_TO_CALC : 0;
        } finally { viewNodeThreadManager.unregisterWritingThread(); }
    }
    public void discardViewNodes() {
        viewNodeThreadManager.registerWritingThread();
        try {
            head = ViewNode.createHeadViewNode();
            startTimeToViewLine.clear();
            endTimeToViewLine.clear();
        } finally { viewNodeThreadManager.unregisterWritingThread(); }
    }
    public Iterator<ViewNode> getViewNodeIter() {
        return head.status == ViewNode.VNStatus.SET
                ? Collections.singletonList(head).iterator()
                : Collections.<ViewNode>emptyList().iterator();
    }
    public static double calcTrDist(int start, int end, Object paths) { return 123; }
    public boolean doViewNodesIntersect(int x1, int y1, int x2, int y2) { return true; }
}
