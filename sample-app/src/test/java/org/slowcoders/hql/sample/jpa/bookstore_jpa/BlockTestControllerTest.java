package org.slowcoders.hql.sample.jpa.bookstore_jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slowcoders.hyperql.sample.SampleHyperQueryApplication;
import org.slowcoders.hyperql.sample.hpss.hpms.service.BlockBasicFilter;
import org.slowcoders.hyperql.sample.hpss.hpms.service.BlockBasicRecord;
import org.slowcoders.hyperql.sample.hpss.hpms.service.BlockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@ActiveProfiles({"postgres"})
@SpringBootTest(classes={SampleHyperQueryApplication.class})
//@WebMvcTest()
@ExtendWith(MockitoExtension.class)
//@RestController
//@RequestMapping("/test/api/blocks")
public class BlockTestControllerTest {

    @Autowired
    private BlockService blockService;

    public BlockTestControllerTest() {

    }

    @PostMapping("/search")
    public List<BlockBasicRecord> testSearch(@RequestBody BlockBasicFilter filter) {
        return blockService.selectList(BlockBasicRecord.class, filter);
    }

    @GetMapping("/{id}")
    public List<BlockBasicRecord> testGetById(@PathVariable("id") String id) {
        BlockBasicFilter filter = new BlockBasicFilter();
        filter.setBlockId(id);
        // 테스트용 기본값 설정 (필요 시 수정)
        filter.setChainId("LTHR");
        filter.setPropertyId("LTWO");
        return blockService.selectList(BlockBasicRecord.class, filter);
    }

    @Test
    public void contextLoads() {
        BlockBasicFilter filter = new BlockBasicFilter();
        filter.setBlockId("2212");
        testSearch(filter);
    }

}
