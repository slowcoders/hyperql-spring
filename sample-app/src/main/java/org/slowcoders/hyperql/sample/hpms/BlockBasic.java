package org.slowcoders.hyperql.sample.hpms;

import org.slowcoders.hyperquery.core.QJoin;
import org.slowcoders.hyperquery.core.QAttribute;
import org.slowcoders.hyperquery.core.QTable;

public interface BlockBasic extends HpmsGlobal {
    QTable table = QTable.of(BlockBasic.class, "hpms.hpbk_block_basic");
    QJoin bookings = QJoin.toMulti(BookingBasic.class, "#.block_id = @.block_id");
    QJoin bookingDetails = QJoin.toMulti(BookingDetail.class, "#bookings.block_id = @.block_id");
    QAttribute bkdd = QAttribute.of("@bookings.@dailyDetail");
}
