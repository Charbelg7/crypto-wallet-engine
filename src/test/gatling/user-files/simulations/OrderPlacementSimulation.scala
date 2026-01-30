import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Gatling load test simulation for order placement.
 * Tests concurrent order placement and measures throughput/latency.
 */
class OrderPlacementSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  // Create user scenario
  val createUser = scenario("Create User")
    .exec(http("create_user")
      .post("/api/v1/users")
      .body(StringBody("""{"email": "user${randomInt()}@test.com", "name": "Test User ${randomInt()}"}"""))
      .check(status.is(201))
      .check(jsonPath("$.id").saveAs("userId")))

  // Deposit scenario
  val deposit = scenario("Deposit")
    .exec(createUser)
    .exec(http("deposit_usdt")
      .post("/api/v1/wallets/deposit?userId=${userId}&currency=USDT")
      .body(StringBody("""{"amount": 100000}"""))
      .check(status.is(200)))

  // Place limit buy order scenario
  val placeLimitBuyOrder = scenario("Place Limit Buy Order")
    .exec(createUser)
    .exec(http("deposit_usdt")
      .post("/api/v1/wallets/deposit?userId=${userId}&currency=USDT")
      .body(StringBody("""{"amount": 100000}"""))
      .check(status.is(200)))
    .repeat(10) {
      exec(http("place_limit_buy_order")
        .post("/api/v1/orders?userId=${userId}")
        .body(StringBody("""{
          "type": "LIMIT",
          "side": "BUY",
          "baseCurrency": "BTC",
          "quoteCurrency": "USDT",
          "price": ${50000 + randomInt(1000)},
          "quantity": ${0.1 + randomDouble() * 0.9}
        }"""))
        .check(status.in(201, 200)))
    }

  // Place limit sell order scenario
  val placeLimitSellOrder = scenario("Place Limit Sell Order")
    .exec(createUser)
    .exec(http("deposit_usdt")
      .post("/api/v1/wallets/deposit?userId=${userId}&currency=USDT")
      .body(StringBody("""{"amount": 100000}"""))
      .check(status.is(200)))
    .exec(http("deposit_btc")
      .post("/api/v1/wallets/deposit?userId=${userId}&currency=BTC")
      .body(StringBody("""{"amount": 10}"""))
      .check(status.is(200)))
    .repeat(10) {
      exec(http("place_limit_sell_order")
        .post("/api/v1/orders?userId=${userId}")
        .body(StringBody("""{
          "type": "LIMIT",
          "side": "SELL",
          "baseCurrency": "BTC",
          "quoteCurrency": "USDT",
          "price": ${51000 + randomInt(1000)},
          "quantity": ${0.1 + randomDouble() * 0.9}
        }"""))
        .check(status.in(201, 200)))
    }

  // Mixed workload scenario
  val mixedWorkload = scenario("Mixed Workload")
    .exec(createUser)
    .exec(http("deposit_usdt")
      .post("/api/v1/wallets/deposit?userId=${userId}&currency=USDT")
      .body(StringBody("""{"amount": 100000}"""))
      .check(status.is(200)))
    .exec(http("deposit_btc")
      .post("/api/v1/wallets/deposit?userId=${userId}&currency=BTC")
      .body(StringBody("""{"amount": 10}"""))
      .check(status.is(200)))
    .exec(http("get_balances")
      .get("/api/v1/wallets/balances?userId=${userId}")
      .check(status.is(200)))
    .repeat(5) {
      exec(http("place_buy_order")
        .post("/api/v1/orders?userId=${userId}")
        .body(StringBody("""{
          "type": "LIMIT",
          "side": "BUY",
          "baseCurrency": "BTC",
          "quoteCurrency": "USDT",
          "price": ${50000 + randomInt(1000)},
          "quantity": 0.1
        }"""))
        .check(status.in(201, 200)))
      .exec(http("place_sell_order")
        .post("/api/v1/orders?userId=${userId}")
        .body(StringBody("""{
          "type": "LIMIT",
          "side": "SELL",
          "baseCurrency": "BTC",
          "quoteCurrency": "USDT",
          "price": ${51000 + randomInt(1000)},
          "quantity": 0.1
        }"""))
        .check(status.in(201, 200)))
      .exec(http("get_orderbook")
        .get("/api/v1/market/orderbook/BTC/USDT")
        .check(status.is(200)))
    }

  // Load test setup
  setUp(
    // Ramp up 50 users over 30 seconds, hold for 2 minutes
    placeLimitBuyOrder.inject(
      rampUsers(50).during(30.seconds),
      constantUsersPerSec(10).during(2.minutes)
    ),
    placeLimitSellOrder.inject(
      rampUsers(50).during(30.seconds),
      constantUsersPerSec(10).during(2.minutes)
    ),
    mixedWorkload.inject(
      rampUsers(20).during(30.seconds),
      constantUsersPerSec(5).during(2.minutes)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(2000), // Max response time < 2s
      global.successfulRequests.percent.gt(95) // Success rate > 95%
    )
}
