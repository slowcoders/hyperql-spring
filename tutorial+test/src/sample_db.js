import { HqlApi } from "./api/hqlApi";

const bookstore_url = "http://localhost:7007/api/hql/bookstore"
export const authorRepo = new HqlApi(`${bookstore_url}/author`);
export const customerRepo = new HqlApi(`${bookstore_url}/customer`);
export const bookRepo = new HqlApi(`${bookstore_url}/book`);
export const publisherRepo = new HqlApi(`${bookstore_url}/publisher`);
export const bookOrderRepo = new HqlApi(`${bookstore_url}/book_order`);
export const customer_friend_repo = new HqlApi(`${bookstore_url}/customer_friend_link`);

export async function initSampleDB() {
    // if (await customerRepo.count() > 0) return;
    const authors = await authorRepo.insertAll(default_authors, 'ignore');
    console.log(authors);
    const customers = await customerRepo.insertAll(default_customers, 'ignore');
    console.log(customers);
    const publishers  = await publisherRepo.insertAll(default_publishers, 'ignore');
    console.log(publishers);
    const books  = await bookRepo.insertAll(default_books, 'ignore');
    console.log(books);
    const bookOrders  = await bookOrderRepo.insertAll(default_book_orders, 'ignore');
    console.log(bookOrders);
    const customer_friend_links = await customer_friend_repo.insertAll(default_friend_map, 'ignore');
    console.log(customer_friend_links);
}

const default_authors = [
    {
        id: 1,
        name: "한강",
    }, {
        id: 2,
        name: "스티븐 킹"
    }, {
        id: 3,
        name: "조엔 롤링"
    }
];

const default_customers = [
    {
        "id": 1000,
        "name": "Luke Skywalker",
        "height": 1.72,
        "mass": 77,
        "memo": {
            "shoeSize": 260,
            "favoriteGenre": ["추리", "미스테리", "SF"],
            "homePlanet": "Tatooine"
        }
    },
    {
        "id": 1001,
        "name": "Darth Vader",
        "height": 2.02,
        "mass": 136,
        "memo": {
            "shoeSize": 370,
            "favoriteGenre": ["공포", "무협", "스릴러"],
            "homePlanet": "Tatooine"
        }
    },
    {
        "id": 1003,
        "name": "Leia Organa",
        "height": 1.5,
        "mass": 49,
        "memo": {
            "shoeSize": 230,
            "homePlanet": "Alderaan",
            "favoriteGenre": ["로맨스", "역사", "판타지"],
        }
    },
    {
        "id": 1004,
        "name": "Groot",
        "height": 1.8,
        "mass": 320,
        "memo": {
            "shoeSize": 850,
            "favoriteGenre": ["무협", "로맨스"],
        }
    },
    {
        "id": 1005,
        "name": "Hobbit",
        "height": 1.19,
        "mass": 50,
        "memo": {
            "shoeSize": 85,
            "favoriteGenre": ["무협", "판타지"],
        }
    },
    {
        "id": 1002,
        "name": "Han Solo",
        "height": 1.8,
        "mass": 80,
        "memo": {
            "shoeSize": 280,
            "favoriteGenre": ["SF", "무협", "판타지"],
            "birth-day": "2001.03.03"
        }
    }
];

const default_publishers = [
    {
        "id": 5000,
        "name": "A 출판사"
    }, {
        "id": 5001,
        "name": "B 출판사"
    }
]

const default_books = [
    {
        "id": 3000,
        "price": 13500,
        "title": "소년이 온다",
        "author_id": 1,
        "publisher_id": 5000,
    },
    {
        "id": 3001,
        "price": 13500,
        "title": "채식주의자",
        "author_id": 1,
        "publisher_id": 5000,
    },
    {
        "id": 3002,
        "price": 17000,
        "title": "미저리",
        "author_id": 2,
        "publisher_id": 5001,
    },
    {
        "id": 3003,
        "price": 15900,
        "title": "그린 마일",
        "author_id": 2,
        "publisher_id": 5001,
    },
    {
        "id": 3004,
        "price": 9800,
        "title": "비밀의 방",
        "author_id": 3,
        "publisher_id": 5000,
    },
    {
        "id": 3005,
        "price": 9800,
        "title": "불의 잔",
        "author_id": 3,
        "publisher_id": 5000,
    },
    {
        "id": 3006,
        "price": 9800,
        "title": "죽음의 성물",
        "author_id": 3,
        "publisher_id": 5000,
    },
    {
        "id": 3007,
        "price": 9800,
        "title": "불사조 기사단",
        "author_id": 3,
        "publisher_id": 5000,
    }
]
  

const default_book_orders = [
    {  customer_id: 1000, book_id: 3000 },
    {  customer_id: 1000, book_id: 3005 },
    {  customer_id: 1001, book_id: 3003 },
    {  customer_id: 1001, book_id: 3004 },
    {  customer_id: 1001, book_id: 3007 },
    {  customer_id: 1002, book_id: 3000 },
    {  customer_id: 1002, book_id: 3004 },
    {  customer_id: 1002, book_id: 3006 },
    {  customer_id: 1003, book_id: 3000 },
    {  customer_id: 1003, book_id: 3001 },
    {  customer_id: 1003, book_id: 3003 },
    {  customer_id: 1003, book_id: 3004 },
    {  customer_id: 1004, book_id: 3006 },
    {  customer_id: 1004, book_id: 3002 },
    {  customer_id: 1005, book_id: 3007 },
];

const default_friend_map = [
    {  customer_id: 1000, friend_id: 1002 },
    {  customer_id: 1000, friend_id: 1003 },

    {  customer_id: 1001, friend_id: 1004 },

    {  customer_id: 1002, friend_id: 1000 },
    {  customer_id: 1002, friend_id: 1003 },

    {  customer_id: 1003, friend_id: 1000 },
    {  customer_id: 1003, friend_id: 1002 },
    {  customer_id: 1003, friend_id: 1005 },

    {  customer_id: 1004, friend_id: 1001 },
    {  customer_id: 1004, friend_id: 1005 },

    {  customer_id: 1005, friend_id: 1003 },
    {  customer_id: 1005, friend_id: 1004 },

]


