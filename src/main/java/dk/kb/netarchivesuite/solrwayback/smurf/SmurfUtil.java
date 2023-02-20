package dk.kb.netarchivesuite.solrwayback.smurf;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.solr.common.util.Pair;

import dk.kb.netarchivesuite.solrwayback.service.dto.smurf.SmurfBuckets;
import dk.kb.netarchivesuite.solrwayback.service.dto.smurf.DateCount;

public class SmurfUtil {

    public static SmurfBuckets generateBuckets(Map<LocalDate, Long> facetsQuery,
            Map<LocalDate, Long> facetsAll, List<Pair<LocalDate, LocalDate>> periods) {

        ArrayList<Double> countPercent = new ArrayList<>();
        ArrayList<DateCount> countTotal = new ArrayList<>();

        boolean anyResults = false;
        
        for (Pair<LocalDate, LocalDate> period : periods) {
            LocalDate date = period.first();
            Long countAll = facetsAll.get(date);
            Long countQuery = facetsQuery.get(date);
            DateCount yearCount = new DateCount();
            if (countQuery == null || countQuery.equals(0L)) {
                countQuery = 0L;
            } else {
                anyResults = true;
            }
            if (countAll == null || countAll.equals(0L)) {
                countAll = 0L; // handle divide by zero
                countPercent.add(0d);
            } else {
                double percent = divide(countQuery, countAll);
                countPercent.add(percent);
            }
            
            yearCount.setDate(date.toString());
            yearCount.setCount(countQuery);
            yearCount.setTotal(countAll);
            countTotal.add(yearCount);
        }


        SmurfBuckets buckets = new SmurfBuckets();
        buckets.setCountPercent(countPercent);
        buckets.setCountsTotal(countTotal);
        buckets.setEmptyResult(!anyResults);

        return buckets;
    }

    private static double divide(long l1, long l2) {
        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern("#.##########");
        double percent = (double) l1 / (double) l2;
        return 100 * Double.parseDouble(df.format(percent));
    }

}