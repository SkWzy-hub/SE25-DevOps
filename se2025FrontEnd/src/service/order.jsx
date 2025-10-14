import { getJson, PREFIX, post } from './common';

export async function getPurchasedOrderList(page = 0, size = 5) {
    const url = `${PREFIX}/order/purchased/${page}/${size}`;
    let res;
    try {
        res = await getJson(url);
    } catch (error) {
        console.error('获取订单列表失败', error);
        res = { content: [], last: true };
    }
    return res;
}

export async function getSoldOrderList(page = 0, size = 5) {
    const url = `${PREFIX}/order/sold/${page}/${size}`;
    let res;
    try {
        res = await getJson(url);
    } catch (error) {
        console.error('获取订单列表失败', error);
        res = { content: [], last: true };
    }
    return res;
}

export async function buyProduct(productId) {
    const url = `${PREFIX}/order/buy/${productId}`;
    let res;
    try {
        res = await post(url);
    } catch (error) {
        console.error('购买商品失败', error);
        res = { success: false, message: '购买失败' };
    }
    return res;
}

export async function getOrderDetail(orderId) {
    const url = `${PREFIX}/order`;
    let res;
    try {
        res = await post(url, { orderId: orderId });
    } catch (error) {
        console.error('获取订单详情失败', error);
        res = { success: false, message: '获取订单详情失败' };
    }
    return res;
}

export async function sellerConfirmOrder(orderId) {
    const url = `${PREFIX}/order/confirm`;
    let res;
    try {
        res = await post(url, { orderId: orderId });
    } catch (error) {
        console.error('确认订单失败', error);
        res = { success: false, message: '确认订单失败' };
    }
    return res;
}

export async function sellerCancelOrder(orderId) {
    const url = `${PREFIX}/order/cancel`;
    let res;
    try {
        res = await post(url, { orderId: orderId });
    } catch (error) {
        console.error('取消订单失败', error);
        res = { success: false, message: '取消订单失败' };
    }
    return res;
}

export async function buyerCompleteOrder(orderId) {
    const url = `${PREFIX}/order/buyer/complete`;
    let res;
    try {
        res = await post(url, { orderId: orderId });
    } catch (error) {
        console.error('完成订单失败', error);
        res = { success: false, message: '完成订单失败' };
    }
    return res;
}

export async function sellerCompleteOrder(orderId) {
    const url = `${PREFIX}/order/seller/complete`;
    let res;
    try {
        res = await post(url, { orderId: orderId });
    } catch (error) {
        console.error('完成订单失败', error);
        res = { success: false, message: '完成订单失败' };
    }
    return res;
}


export async function sellerCreditOrder(orderId, rating) {
    const url = `${PREFIX}/order/seller/credit`;
    let res;
    try {
        res = await post(url, {
            credit: rating,
            orderId: orderId
        });
    } catch (error) {
        console.error('信用评价失败', error);
        res = { success: false, message: '信用评价失败' };
    }
    return res;
}

export async function buyerCreditOrder(orderId, rating) {
    const url = `${PREFIX}/order/buyer/credit`;
    let res;
    try {
        res = await post(url, {
            credit: rating,
            orderId: orderId
        });
    } catch (error) {
        console.error('信用评价失败', error);
        res = { success: false, message: '信用评价失败' };
    }
    return res;
}