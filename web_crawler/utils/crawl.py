import random

import requests
from bs4 import BeautifulSoup

# 常用真实浏览器的User-Agent列表
user_agents = [
    # Chrome 浏览器 User-Agent
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_1_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.216 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",

    # Firefox 浏览器 User-Agent
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.0; rv:121.0) Gecko/20100101 Firefox/121.0",

    # Safari 浏览器 User-Agent
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0_1) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1",

    # Edge 浏览器 User-Agent
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36 Edg/121.0.0.0",
]
headers = {
        "User-Agent": random.choice(user_agents)  # 随机选择一个User-Agent
    }

# 每日快讯
def crawl_news1():
    url = "https://info.texnet.com.cn/daynews_list--1.html"

    res = requests.get(url, headers=headers)
    res.encoding = 'utf-8'
    soup = BeautifulSoup(res.text, 'html.parser')

    news_items = soup.select('div.content-list > ul > li:has(a)')

    news_data = []
    for item in news_items:
        title_tag = item.find('a')
        title = title_tag.get('title')
        link = title_tag.get('href') if title_tag else ""
        if link and not link.startswith('http'):
            link = "https://info.texnet.com.cn" + link
        time_tag = item.find('span')
        time = time_tag.get_text(strip=True) if time_tag else ""

        news_data.append({"title": title, "link": link, "publishTime": time})

    return news_dataR

# 综合专区
def crawl_news2():
    url = "https://info.texnet.com.cn/list-19--1.html" # 综合专区

    res = requests.get(url, headers=headers)
    res.encoding = 'utf-8'
    soup = BeautifulSoup(res.text, 'html.parser')

    news_items = soup.select('div.content-list > ul > li:has(a)') # 1,2
    # news_items = soup.select('div.content-list2 > ul > li:has(a)') # 3

    news_data = []
    for item in news_items:
        title_tag = item.find('a')
        title = title_tag.get('title')
        link = title_tag.get('href') if title_tag else ""
        if link and not link.startswith('http'):
            link = "https://info.texnet.com.cn" + link
        time_tag = item.find('span')
        time = time_tag.get_text(strip=True) if time_tag else ""

        news_data.append({"title": title, "link": link, "publishTime": time})

    return news_data

# 服装动态
def crawl_news3():
    url = "https://info.texnet.com.cn/fz_list-11-4.html"

    res = requests.get(url, headers=headers)
    res.encoding = 'utf-8'
    soup = BeautifulSoup(res.text, 'html.parser')

    news_items = soup.select('div.content-list2 > ul > li:has(a)')

    news_data = []
    for item in news_items:
        title_tag = item.find('a')
        title = title_tag.get('title')
        link = title_tag.get('href') if title_tag else ""
        if link and not link.startswith('http'):
            link = "https://info.texnet.com.cn" + link
        time_tag = item.find('span')
        time = time_tag.get_text(strip=True) if time_tag else ""

        news_data.append({"title": title, "link": link, "publishTime": time})

    return news_data

# 原材料专区
def crawl_news4():
    url = "https://info.texnet.com.cn/list-11--1.html"

    res = requests.get(url, headers=headers)
    res.encoding = 'utf-8'
    soup = BeautifulSoup(res.text, 'html.parser')

    news_items = soup.select('div.content-list > ul > li:has(a)')

    news_data = []
    for item in news_items:
        title_tag = item.find('a')
        title = title_tag.get('title')
        link = title_tag.get('href') if title_tag else ""
        if link and not link.startswith('http'):
            link = "https://info.texnet.com.cn" + link
        time_tag = item.find('span')
        time = time_tag.get_text(strip=True) if time_tag else ""

        news_data.append({"title": title, "link": link, "publishTime": time})

    return news_data

# 会展快讯
def crawl_news5():
    url = "https://expo.texnet.com.cn/news/"

    res = requests.get(url, headers=headers)
    res.encoding = 'utf-8'
    soup = BeautifulSoup(res.text, features='html.parser')

    news_items = soup.select('tr > td.t150grey > a')
    news_times = soup.select('span.g666666')

    news_data = [{
        "title": item.get_text(strip=True),
        "link": item.get('href'),
        "publishTime": news_times[idx].get_text(strip=True).strip('()')
    } for idx, item in enumerate(news_items)]

    return news_data

# 商品动态
def crawl_news6():
    url = "https://tex.100ppi.com/news/list---1.html"

    response = requests.get(url, headers=headers)
    response.encoding = 'utf-8'
    soup = BeautifulSoup(response.text, 'html.parser')

    news_data = []
    news_items = soup.select('ul > li > a.blueq:nth-of-type(2)')
    for item in news_items:
        title = item.get_text(strip=True)
        link = item.get('href')
        if not link.startswith("http"):
            link = "https://tex.100ppi.com" + link
        time_element = item.find_next_sibling('span')
        time = time_element.get_text(strip=True) if time_element else "N/A"

        news_data.append({
            "title": title,
            "link": link,
            "publishTime": time
        })

    return news_data

# 爬取网页内容
def crawl_news_context(url):
    res = requests.get(url, headers=headers)
    res.encoding = 'utf-8'
    soup = BeautifulSoup(res.text, 'html.parser')

    paragraphs = soup.select('div.detail-text > div > p > p')
    content = '\n'.join([p.get_text(strip=True) for p in paragraphs])

    return content

if __name__ == "__main__":
    # crawl_news1()
    # crawl_news2()
    # crawl_news3()
    # crawl_news4()
    # crawl_news5()
    # crawl_news6()
    crawl_news_context("https://info.texnet.com.cn/detail-1022422.html")