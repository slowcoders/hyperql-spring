package org.slowcoders.hyperquery.hpms;

import org.slowcoders.hyperquery.core.Q;
import org.slowcoders.hyperquery.core.QJoin;

@Q.From("hpms.hpbk_booking_basic")
public
interface BookingBasic extends HpmsGlobal {
    QJoin bookingTypeCd = QJoin.toSingle(bookingTypeCdView, "#.detail_cd = @.booking_type_cd");
    QJoin dailyDetail = QJoin.toMulti(BookingDetail.class, "#.booking_seq = @.booking_seq");
}
