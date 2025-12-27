package org.slowcoders.hyperql.sample.hpms;

import org.slowcoders.hyperquery.core.Q;
import org.slowcoders.hyperquery.core.QView;

public interface HpmsGlobal extends Q {
    QView bookingTypeCdView = QView.of("""
                select *, coalesce(cd.reference_character_value1, 'N') as _deduct_yn
                from fwcm.fwcm_cd_detail cd
                where cd.common_cd   = 'BOOKING_TYPE_CD'
            """);
}
