package org.slowcoders.hyperql.sample.hpms;

import org.slowcoders.hyperquery.core.*;

import java.util.EnumSet;

@Q.From("hpms.hpbk_block_detail")
public
interface BookingDetail extends HpmsGlobal {
    QJoin block = QJoin.toSingle(BlockBasic.class, "#.block_id = @.block_id");
    QJoin booking = QJoin.toSingle(BookingBasic.class, "#.booking_seq = @.booking_seq");
//    QJoin roomTypeCode = QJoin.toSingle(new QTable("hpms.hpbk_room_type_code"),
//            "#.room_type_code = @.room_type_code");

    QJoin isDayUseOcc2 = QJoin.toSingle(QView.of("select ..."), "#.id = @.id");

    // 가상 Column 내에서 다른 가상 Column 참조 시 내용 치환!!
    QAttribute _deduct_yn = QAttribute.of("@booking.@bookingTypeCd._deduct_yn");
    QAttribute _day_use_yn = QAttribute.of("case when @booking.arrival_date    = @booking.departure_date        then 'Y' else 'N' end");
    QAttribute _due_in_yn = QAttribute.of("case when @booking.arrival_date    = @.room_booking_date            then 'Y' else 'N' end");
    QAttribute _due_out_yn = QAttribute.of("case when @booking.departure_date  = @.room_booking_date            then 'Y' else 'N' end");
    QAttribute deduct_cnt = QAttribute.of("case when @._deduct_yn = 'Y'                           then @.room_count end");
    QAttribute non_deduct_cnt = QAttribute.of("case when @._deduct_yn = 'N'                           then @.room_count end");
    QAttribute arrival_cnt = QAttribute.of("case when @._deduct_yn = 'Y' and @._due_in_yn   = 'Y'  then @.room_count end");
    QAttribute departure_cnt = QAttribute.of("case when @._deduct_yn = 'Y' and @._due_out_yn  = 'Y'  then @.room_count end");

}
