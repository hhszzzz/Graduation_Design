from flask import Flask, jsonify, request
from flask_cors import CORS
from utils import crawl

app = Flask(__name__)
CORS(app)  # 允许跨域访问

# 定义REST接口
@app.route('/api/crawl1', methods=['GET'])
def api_crawl1():
    data = crawl.crawl_news1()
    return jsonify(data)
@app.route('/api/crawl2', methods=['GET'])
def api_crawl2():
    data = crawl.crawl_news2()
    return jsonify(data)
@app.route('/api/crawl3', methods=['GET'])
def api_crawl3():
    data = crawl.crawl_news3()
    return jsonify(data)
@app.route('/api/crawl4', methods=['GET'])
def api_crawl4():
    data = crawl.crawl_news4()
    return jsonify(data)
@app.route('/api/crawl5', methods=['GET'])
def api_crawl5():
    data = crawl.crawl_news5()
    return jsonify(data)
@app.route('/api/crawl6', methods=['GET'])
def api_crawl6():
    data = crawl.crawl_news6()
    return jsonify(data)
@app.route('/api/crawl_context', methods=['GET'])
def api_crawl_context():
    url = request.args.get('url')
    context = crawl.crawl_news_context(url)
    return jsonify(context)

if __name__ == '__main__':
    from waitress import serve
    serve(app, host='0.0.0.0', port=9999)
