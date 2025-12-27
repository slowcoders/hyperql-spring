import {beforeAll, describe, expect, test} from '@jest/globals';
import { customerRepo, initSampleDB } from '@/sample_db'

describe('And operation', () => {
  let last_count;
  const filter = {}

    initSampleDB();

  beforeAll(async () => {
    const res = await customerRepo.find();
    last_count = res.content.length;

    const res2 = await customerRepo.find(null);
    expect(res2.content.length).toBe(last_count)

    const res3 = await customerRepo.find(filter);
    expect(res3.content.length).toBe(last_count)
  })

  test.each([
    { attr: "height >", value: 1.2},
    { attr: "height <", value: 2.0},
    { attr: "mass >", value: 60 },
    { attr: "memo.shoeSize <", value: 300 },
    { attr: "memo.homePlanet", value: "Tatooine" }
  ]) ('And 조건 테스트', async ({attr, value}) => {
    filter[attr] = value;
    const res = await customerRepo.find(filter);
    const customers = res.content;
    expect(customers.length).toBeLessThan(last_count);
    last_count = customers.length;
  });  
});

