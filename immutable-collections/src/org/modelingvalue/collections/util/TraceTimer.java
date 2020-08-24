//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2019 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus, Ronald Krijgsheld                                                                           ~
// Contributors:                                                                                                       ~
//     Arjan Kok, Carel Bast                                                                                           ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

package org.modelingvalue.collections.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingFormatArgumentException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public final class TraceTimer {
    private static final String  REST                     = "<REST>";
    private static final int     MIL                      = 1000000;
    private static final boolean TRACE_TIME               = Boolean.getBoolean("TRACE_TIME");
    private static final boolean TRACE_LOG                = Boolean.getBoolean("TRACE_LOG");
    private static final boolean TRACE_TIME_STEP          = Boolean.getBoolean("TRACE_TIME_STEP");
    private static final int     TRACE_TIME_DUMP_INTERVAL = Integer.getInteger("TRACE_TIME_DUMP_INTERVAL", 10) * 1000;
    private static final int     TRACE_TIME_DUMP_NR       = Integer.getInteger("TRACE_TIME_DUMP_NR", 100);
    private static final boolean TRACE_TIME_CLEAR         = Boolean.getBoolean("TRACE_TIME_CLEAR");
    private static final String  TRACE_TIME_TOTAL         = System.getProperties().getProperty("TRACE_TIME_TOTAL");
    private static final Pattern TRACE_TIME_TOTAL_PATTERN = TRACE_TIME_TOTAL != null ? Pattern.compile(TRACE_TIME_TOTAL) : null;
    private static final String  TRACE_PATTERN            = System.getProperties().getProperty("TRACE_PATTERN");
    private static final Pattern TRACE_PATTERN_PATTERN    = TRACE_PATTERN != null ? Pattern.compile(TRACE_PATTERN) : null;

    private static final Comparator<Map.Entry<String, Long>> COMPARATOR = (o1, o2) -> o2.getValue().compareTo(o1.getValue());

    private static final List<TraceTimer>        ALL_TIMERS = new ArrayList<>();
    private static final List<TraceLog>          ALL_LOGS   = new ArrayList<>();
    private static final ThreadLocal<TraceTimer> TIMER      = ThreadLocal.withInitial(() -> {
        TraceTimer tt = new TraceTimer(Thread.currentThread());
        synchronized (ALL_TIMERS) {
            ALL_TIMERS.add(tt);
            ALL_TIMERS.sort(Comparator.comparing(o -> o.thread.getName()));
        }
        tt.init();
        return tt;
    });

    private static Timer   dumpTimer;
    private static boolean timersChanged;
    private static boolean logsChanged;

    static {
        if (TRACE_TIME || TRACE_LOG) {
            initTimer();
        }
    }

    private static void initTimer() {
        dumpTimer = new Timer("Timer#TraceTimer", true);
        dumpTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    dumpAll();
                } catch (Throwable t) {
                    System.err.println("Throwable in TraceTimer:");
                    t.printStackTrace();
                }
            }
        }, TRACE_TIME_DUMP_INTERVAL, TRACE_TIME_DUMP_INTERVAL);
    }

    private long                       time;
    private long                       grandTotal;
    private final Deque<String>        queue   = new LinkedList<>();
    private final Map<String, Long>    total   = new LinkedHashMap<>();
    private final Map<String, Integer> count   = new LinkedHashMap<>();
    private final Thread               thread;

    private TraceTimer(Thread thread) {
        this.thread = thread;
    }

    private void init() {
        queue.offerLast(REST);
        time = System.nanoTime();
    }

    private synchronized void clear() {
        grandTotal = 0L;
        queue.clear();
        total.clear();
        count.clear();
        init();
    }

    private synchronized void begin(String name) {
        long now = System.nanoTime();
        if (TRACE_PATTERN_PATTERN == null || TRACE_PATTERN_PATTERN.matcher(name).matches()) {
            String current = queue.peekLast();
            queue.offerLast(name);
            long delta = now - time;
            total(current, delta);
            if (TRACE_TIME_STEP) {
                System.out.printf("%-32s BEGIN %-44s at %16dns\n", thread.getName(), name, now);
            }
            timersChanged = true;
            time = System.nanoTime();
        } else {
            time += System.nanoTime() - now;
        }
    }

    private synchronized void end(String name) {
        long now = System.nanoTime();
        if (TRACE_PATTERN_PATTERN == null || TRACE_PATTERN_PATTERN.matcher(name).matches()) {
            if (queue.size() > 1) {
                count(name);
                long delta = now - time;
                total(name, delta);
                String last = queue.pollLast();
                if (TRACE_TIME_STEP) {
                    System.out.printf("%-32s   END %-44s at %16dns\n", thread.getName(), name, now);
                }
                if (!name.equals(last)) {
                    System.err.println("Trace Timer begin/end mis match: '" + last + "' <> '" + name + "'");
                }
            }
            timersChanged = true;
            time = System.nanoTime();
        } else {
            time += System.nanoTime() - now;
        }
    }

    private void count(String name) {
        Integer c = count.get(name);
        c = c != null ? c + 1 : 1;
        count.put(name, c);
    }

    private void total(String name, long delta) {
        //noinspection StringEquality
        if (name != REST) {
            grandTotal += delta;
        }
        Long t = total.get(name);
        t = t != null ? t + delta : delta;
        total.put(name, t);
    }

    private synchronized long sum(Map<String, Long> sumTotal, Map<String, Integer> sumCount) {
        for (Entry<String, Long> entry : total.entrySet()) {
            Long tot = sumTotal.get(entry.getKey());
            sumTotal.put(entry.getKey(), tot != null ? tot + entry.getValue() : entry.getValue());
        }
        for (Entry<String, Integer> entry : count.entrySet()) {
            Integer cnt = sumCount.get(entry.getKey());
            sumCount.put(entry.getKey(), cnt != null ? cnt + entry.getValue() : entry.getValue());
        }
        return grandTotal;
    }

    private synchronized void dump(List<String> log) {
        dump(log, thread.getName(), grandTotal, total, count, 1);
    }

    private static void dump(List<String> log, String name, long grandTotal, Map<String, Long> total, Map<String, Integer> count, int nrOffThreads) {
        if (grandTotal > 0L) {
            List<Map.Entry<String, Long>> list = new ArrayList<>(total.entrySet());
            list.sort(COMPARATOR);
            boolean preDone = false;
            for (int i = 0; i < TRACE_TIME_DUMP_NR && i < list.size(); i++) {
                Map.Entry<String, Long> entry = list.get(i);
                Long                    tot   = total.get(entry.getKey());
                tot = tot != null ? tot : 0L;
                long prc = 100L * tot / grandTotal;
                if (!preDone) {
                    preDone = true;
                    log.add(String.format("------------%-32s%10dms--------------------", name, grandTotal / MIL).replace(' ', '-'));
                }
                Integer cnt = count.get(entry.getKey());
                cnt = cnt != null ? cnt : nrOffThreads;
                log.add(String.format(" %-35s%7d#%10dms%10dmus/#%4d%%", entry.getKey(), cnt, tot / MIL, tot / cnt / 1000, prc));
            }
        }
    }

    public static void traceBegin(String name) {
        if (TRACE_TIME) {
            TIMER.get().begin(name);
        }
    }

    public static void traceEnd(String name) {
        if (TRACE_TIME) {
            TIMER.get().end(name);
        }
    }

    public static void clearAll() {
        if (TRACE_TIME) {
            synchronized (ALL_TIMERS) {
                dumpTimer.cancel();
            }
            timersChanged = false;
            TraceTimer[] all;
            synchronized (ALL_TIMERS) {
                all = ALL_TIMERS.toArray(new TraceTimer[0]);
            }
            for (final TraceTimer tt : all) {
                tt.clear();
            }
            synchronized (ALL_TIMERS) {
                initTimer();
            }
        }
    }

    public static void dumpAll() {
        List<String> log = new ArrayList<>();
        if (TRACE_TIME && timersChanged) {
            timersChanged = false;
            TraceTimer[] all;
            synchronized (ALL_TIMERS) {
                all = ALL_TIMERS.toArray(new TraceTimer[0]);
            }
            if (TRACE_TIME_TOTAL != null) {
                long                 grandTotal  = 0L;
                Map<String, Long>    total       = new LinkedHashMap<>();
                Map<String, Integer> count       = new LinkedHashMap<>();
                int                  nrOfThreads = 0;
                for (final TraceTimer tt : all) {
                    if (TRACE_TIME_TOTAL_PATTERN.matcher(tt.thread.getName()).matches()) {
                        grandTotal += tt.sum(total, count);
                        nrOfThreads++;
                    } else {
                        tt.dump(log);
                    }
                }
                dump(log, "Total of " + nrOfThreads + " " + TRACE_TIME_TOTAL + " threads", grandTotal, total, count, nrOfThreads);
            } else {
                for (final TraceTimer tt : all) {
                    tt.dump(log);
                }
            }
            if (TRACE_TIME_CLEAR) {
                clearAll();
            }
        }
        if (TRACE_LOG && logsChanged) {
            TraceLog[] allLogs;
            synchronized (ALL_LOGS) {
                allLogs = ALL_LOGS.toArray(new TraceLog[0]);
                logsChanged = false;
                ALL_LOGS.clear();
            }
            for (TraceLog l : allLogs) {
                l.dump(log);
            }
        }
        if (!log.isEmpty()) {
            log.forEach(System.err::println);
        }
    }

    public static void traceLog(String format, Object... args) {
        if (TRACE_LOG) {
            synchronized (ALL_LOGS) {
                logsChanged = true;
                ALL_LOGS.add(new TraceLog(format, args));
            }
        }
    }

    private static class TraceLog {
        private final String   format;
        private final Object[] args;
        private final long     nanoDelta;
        private final Thread   thread;

        private static final String PRE_FORMAT = "%-30s %,15d|";
        private static final String SHIFT      = String.format("\n%" + (String.format(PRE_FORMAT, "", 0).length() - 1) + "s|", "");
        private static final long   T0_NANO    = System.nanoTime();
        private static       long   last       = System.nanoTime();

        public TraceLog(String format, Object... args) {
            this.format = format;
            this.args = args;
            long t = System.nanoTime();
            nanoDelta = t - last;
            last = t;
            thread = Thread.currentThread();
        }

        public void dump(List<String> log) {
            try {
                String pre = String.format(PRE_FORMAT, thread.getName(), nanoDelta);
                String msg = String.format(format, args).replace("\n", SHIFT);
                log.add(pre + msg);
            } catch (MissingFormatArgumentException e) {
                log.add("OOPS bad format: " + format);
            }
        }
    }
}
