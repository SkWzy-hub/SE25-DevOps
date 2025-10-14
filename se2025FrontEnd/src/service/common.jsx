// 动态获取token，确保每次都是最新的
function getToken() {
    return localStorage.getItem('token');
}

export function setToken(newToken) {
    if (newToken) {
        localStorage.setItem('token', newToken);
    } else {
        localStorage.removeItem('token');
    }
}

export async function getJson(url) {
    let headers = {};
    const token = getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    let res = await fetch(url, { 
        method: "GET", 
        headers 
    });
    return res.json();
}

export async function get(url) {
    let headers = {};
    const token = getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    let res = await fetch(url, { 
        method: "GET", 
        headers 
    });
    return res;
}

export async function put(url, data) {
    let headers = {
        'Content-Type': 'application/json'
    };
    const token = getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    let opts = {
        method: "PUT",
        body: JSON.stringify(data),
        headers
    };
    let res = await fetch(url, opts);
    return res.json();
}

export async function del(url, data) {
    let headers = {
        'Content-Type': 'application/json'
    };
    const token = getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    let opts = {
        method: "DELETE",
        headers
    };
    if (data) {
        opts.body = JSON.stringify(data);
    }
    let res = await fetch(url, opts);
    return res.json();
}

export async function post(url, data) {
    let headers = {
        'Content-Type': 'application/json'
    };
    const token = getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    let opts = {
        method: "POST",
        body: JSON.stringify(data),
        headers
    };
    let res = await fetch(url, opts);
    return res.json();
}

// Token服务
export const TokenService = {
    setToken,
    getToken,
    removeToken: () => setToken(null)
};

// 在开发环境使用Vite代理，生产环境使用环境变量
export const BASEURL = import.meta.env.VITE_BASE_URL ?? '';
export const PREFIX = `${BASEURL}/api`;
export const API_DOCS_URL = `${BASEURL}/api-docs`;
export const DUMMY_RESPONSE = {
    ok: false,
    message: "网络错误！"
}