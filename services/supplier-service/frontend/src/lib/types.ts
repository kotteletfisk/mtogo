  export interface Order {
    orderId: string;
    customerId: number;
    supplierId: number;
    orderCreated: string;
    orderUpdated: string;
    orderlineDTOs: OrderLine[];
    orderStatus: "created" | string;
  }

  export interface OrderLine {
    orderLineId: number;
    orderId: string;
    itemId: number;
    priceSnapshot: number;
    amount: number;
  }

  export interface APIException {
    statusCode: number,
    message: string,
    timestamp: string,
  }