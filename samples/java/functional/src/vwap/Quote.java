/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015  
 */
package vwap;

import java.math.BigDecimal;

import com.ibm.streams.operator.Tuple;
import com.ibm.streamsx.topology.TStream;
import com.ibm.streamsx.topology.function7.Function;
import com.ibm.streamsx.topology.spl.SPLStream;

public class Quote extends Ticker {
    /**
     * 
     */
    private static final long serialVersionUID = -8355672072795288713L;
    final BigDecimal bidprice;
    final BigDecimal askprice;
    final BigDecimal asksize;

    public Quote(Tuple tuple) {
        super(tuple);
        bidprice = tuple.getBigDecimal("bidprice");
        askprice = tuple.getBigDecimal("askprice");
        asksize = tuple.getBigDecimal("asksize");
    }

    public String toString() {
        return "QUOTE: " + getTicker() + " bidprice=" + bidprice + " askprice="
                + askprice + " asksize=" + asksize;
    }

    public static TStream<Quote> getQuotes(SPLStream tradeQuotes) {
        return tradeQuotes.transform(new Function<Tuple, Quote>() {
            @Override
            public Quote apply(Tuple tuple) {
                if ("Quote".equals(tuple.getString("ttype")))
                    return new Quote(tuple);
                return null;
            }
        }, Quote.class);
    }
}
