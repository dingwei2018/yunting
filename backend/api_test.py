#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
北京云听音频精修系统 - 接口测试脚本
用于自动化测试所有API接口
"""

import requests
import json
import time
from typing import Dict, Any, Optional

# 配置信息
BASE_URL = "http://localhost:8080/v1"  # 根据实际部署地址修改
TIMEOUT = 30  # 请求超时时间（秒）

# 全局变量，用于存储测试过程中的数据
test_data = {
    "task_id": None,
    "sentence_id": None,
    "sentence_ids": [],
    "rule_id": None,
    "merge_id": None,
    "voice_id": "xiaoyan"
}


class APITester:
    """API测试类"""
    
    def __init__(self, base_url: str = BASE_URL):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            "Content-Type": "application/json",
            "Accept": "application/json"
        })
    
    def log(self, message: str, level: str = "INFO"):
        """打印日志"""
        timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
        print(f"[{timestamp}] [{level}] {message}")
    
    def request(self, method: str, endpoint: str, params: Optional[Dict] = None, 
                data: Optional[Dict] = None, expected_code: int = 10200) -> Optional[Dict]:
        """发送HTTP请求"""
        url = f"{self.base_url}{endpoint}"
        
        try:
            if method.upper() == "GET":
                response = self.session.get(url, params=params, timeout=TIMEOUT)
            elif method.upper() == "POST":
                response = self.session.post(url, params=params, json=data, timeout=TIMEOUT)
            elif method.upper() == "DELETE":
                response = self.session.delete(url, params=params, timeout=TIMEOUT)
            else:
                self.log(f"不支持的HTTP方法: {method}", "ERROR")
                return None
            
            # 解析响应
            try:
                result = response.json()
            except:
                result = {"raw_response": response.text}
            
            # 检查状态码
            if result.get("code") == expected_code:
                self.log(f"✓ {method} {endpoint} - 成功", "SUCCESS")
                return result
            else:
                self.log(f"✗ {method} {endpoint} - 失败: {result.get('message', '未知错误')}", "ERROR")
                self.log(f"  响应: {json.dumps(result, ensure_ascii=False, indent=2)}", "DEBUG")
                return result
                
        except requests.exceptions.Timeout:
            self.log(f"✗ {method} {endpoint} - 请求超时", "ERROR")
            return None
        except requests.exceptions.RequestException as e:
            self.log(f"✗ {method} {endpoint} - 请求异常: {str(e)}", "ERROR")
            return None
    
    # ==================== 任务管理接口 ====================
    
    def test_create_task(self):
        """测试：创建任务并自动拆句"""
        self.log("=" * 60)
        self.log("测试：创建任务并自动拆句")
        self.log("=" * 60)
        
        data = {
            "content": "这是一段测试文本。用于验证系统功能。支持最多10000字。"
        }
        
        result = self.request("POST", "/tasks", data=data)
        if result and result.get("code") == 10200:
            task_data = result.get("data", {})
            test_data["task_id"] = task_data.get("task_id")
            if task_data.get("sentences"):
                test_data["sentence_id"] = task_data["sentences"][0].get("sentence_id")
                test_data["sentence_ids"] = [s.get("sentence_id") for s in task_data["sentences"]]
            self.log(f"创建任务成功，task_id: {test_data['task_id']}")
            return True
        return False
    
    def test_get_task_detail(self):
        """测试：获取任务详情"""
        if not test_data.get("task_id"):
            self.log("跳过：task_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：获取任务详情")
        self.log("=" * 60)
        
        params = {"taskid": test_data["task_id"]}
        result = self.request("GET", f"/tasks/taskid={test_data['task_id']}")
        return result is not None
    
    def test_get_task_list(self):
        """测试：获取任务列表"""
        self.log("=" * 60)
        self.log("测试：获取任务列表")
        self.log("=" * 60)
        
        params = {"page": 1, "page_size": 20}
        result = self.request("GET", "/tasks", params=params)
        return result is not None
    
    # ==================== 句子管理接口 ====================
    
    def test_get_sentence_list(self):
        """测试：获取句子列表"""
        if not test_data.get("task_id"):
            self.log("跳过：task_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：获取句子列表")
        self.log("=" * 60)
        
        params = {"taskid": test_data["task_id"]}
        result = self.request("GET", "/tasks/sentences", params=params)
        return result is not None
    
    def test_get_sentence_detail(self):
        """测试：获取句子详情"""
        if not test_data.get("sentence_id"):
            self.log("跳过：sentence_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：获取句子详情")
        self.log("=" * 60)
        
        params = {"sentenceid": test_data["sentence_id"]}
        result = self.request("GET", "/sentences/info", params=params)
        return result is not None
    
    def test_delete_sentence(self):
        """测试：删除句子"""
        if not test_data.get("sentence_id"):
            self.log("跳过：sentence_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：删除句子（注意：此操作会删除句子）")
        self.log("=" * 60)
        
        params = {"sentenceid": test_data["sentence_id"]}
        result = self.request("DELETE", "/sentences", params=params)
        return result is not None
    
    # ==================== 语音合成接口 ====================
    
    def test_synthesize_sentence(self):
        """测试：合成单个句子"""
        if not test_data.get("sentence_id"):
            self.log("跳过：sentence_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：合成单个句子")
        self.log("=" * 60)
        
        params = {"sentenceid": test_data["sentence_id"]}
        data = {
            "voice_id": test_data["voice_id"],
            "speech_rate": 0,
            "volume": 0,
            "pitch": 0
        }
        result = self.request("POST", "/sentences/synthesize", params=params, data=data)
        return result is not None
    
    def test_resynthesize_sentence(self):
        """测试：重新合成句子"""
        if not test_data.get("sentence_id"):
            self.log("跳过：sentence_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：重新合成句子")
        self.log("=" * 60)
        
        params = {"sentenceid": test_data["sentence_id"]}
        data = {
            "voice_id": test_data["voice_id"],
            "speech_rate": 10,
            "volume": 5,
            "pitch": -2
        }
        result = self.request("POST", "/sentences/resynthesize", params=params, data=data)
        return result is not None
    
    def test_batch_synthesize(self):
        """测试：批量合成"""
        if not test_data.get("task_id") or not test_data.get("sentence_ids"):
            self.log("跳过：task_id或sentence_ids不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：批量合成")
        self.log("=" * 60)
        
        params = {"taskid": test_data["task_id"]}
        data = {
            "voice_id": test_data["voice_id"],
            "speech_rate": 0,
            "volume": 0,
            "sentence_ids": test_data["sentence_ids"][:3]  # 只合成前3个
        }
        result = self.request("POST", "/tasks/synthesize", params=params, data=data)
        return result is not None
    
    def test_get_synthesis_status(self):
        """测试：查询合成状态"""
        if not test_data.get("task_id"):
            self.log("跳过：task_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：查询合成状态")
        self.log("=" * 60)
        
        params = {"taskid": test_data["task_id"]}
        result = self.request("GET", "/synthesis/tasks", params=params)
        return result is not None
    
    # ==================== 参数调整接口 ====================
    
    def test_adjust_parameters(self):
        """测试：调整参数"""
        if not test_data.get("sentence_id"):
            self.log("跳过：sentence_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：调整参数")
        self.log("=" * 60)
        
        params = {"sentenceid": test_data["sentence_id"]}
        data = {
            "content": "修改后的句子内容",
            "speech_rate": 10,
            "volume": 5,
            "pitch": -2,
            "voice_id": test_data["voice_id"],
            "pauses": [
                {"position": 5, "duration": 500, "type": 1}
            ]
        }
        result = self.request("POST", "/sentences/settings", params=params, data=data)
        return result is not None
    
    def test_adjust_parameters_with_insert(self):
        """测试：调整参数（包含向下插入句子）"""
        if not test_data.get("sentence_id"):
            self.log("跳过：sentence_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：调整参数（包含向下插入句子）")
        self.log("=" * 60)
        
        params = {"sentenceid": test_data["sentence_id"]}
        data = {
            "insert_sentences": [
                {
                    "parent_id": test_data["sentence_id"],
                    "content": "插入的测试文本",
                    "voice_id": test_data["voice_id"]
                }
            ]
        }
        result = self.request("POST", "/sentences/settings", params=params, data=data)
        return result is not None
    
    def test_adjust_parameters_with_rebreak(self):
        """测试：调整参数（包含重新断句）"""
        if not test_data.get("sentence_id"):
            self.log("跳过：sentence_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：调整参数（包含重新断句）")
        self.log("=" * 60)
        
        params = {"sentenceid": test_data["sentence_id"]}
        data = {
            "rebreak_sentence": {
                "breaking_standard_id": 2,
                "char_count": 10
            }
        }
        result = self.request("POST", "/sentences/settings", params=params, data=data)
        return result is not None
    
    # ==================== 阅读规范接口 ====================
    
    def test_create_reading_rule(self):
        """测试：创建阅读规范"""
        if not test_data.get("task_id"):
            self.log("跳过：task_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：创建阅读规范")
        self.log("=" * 60)
        
        data = {
            "task_id": test_data["task_id"],
            "pattern": "2025",
            "rule_type": "数字读法",
            "rule_value": "二零二五",
            "scope": 2
        }
        result = self.request("POST", "/reading-rules", data=data)
        if result and result.get("code") == 10200:
            rule_data = result.get("data", {})
            test_data["rule_id"] = rule_data.get("id")
            self.log(f"创建阅读规范成功，rule_id: {test_data['rule_id']}")
            return True
        return False
    
    def test_get_reading_rules(self):
        """测试：获取阅读规范列表"""
        if not test_data.get("task_id"):
            self.log("跳过：task_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：获取阅读规范列表")
        self.log("=" * 60)
        
        params = {"task_id": test_data["task_id"], "scope": 2}
        result = self.request("GET", "/reading-rules", params=params)
        return result is not None
    
    def test_apply_reading_rule(self):
        """测试：应用阅读规范"""
        if not test_data.get("rule_id") or not test_data.get("task_id"):
            self.log("跳过：rule_id或task_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：应用阅读规范")
        self.log("=" * 60)
        
        params = {
            "ruleid": test_data["rule_id"],
            "taskid": test_data["task_id"]
        }
        result = self.request("POST", "/reading-rules/apply", params=params)
        return result is not None
    
    # ==================== 音频编辑接口 ====================
    
    def test_merge_audio(self):
        """测试：合并音频"""
        if not test_data.get("task_id"):
            self.log("跳过：task_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：合并音频")
        self.log("=" * 60)
        
        params = {"taskid": test_data["task_id"]}
        data = {
            "sentence_ids": test_data.get("sentence_ids", [])
        }
        result = self.request("POST", "/tasks/audio/merge", params=params, data=data)
        if result and result.get("code") == 10200:
            merge_data = result.get("data", {})
            test_data["merge_id"] = merge_data.get("merge_id")
            self.log(f"合并音频成功，merge_id: {test_data['merge_id']}")
            return True
        return result is not None
    
    def test_get_merge_status(self):
        """测试：查询合并状态"""
        if not test_data.get("merge_id"):
            self.log("跳过：merge_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：查询合并状态")
        self.log("=" * 60)
        
        params = {"mergeid": test_data["merge_id"]}
        result = self.request("GET", "/audio/merges/merge", params=params)
        return result is not None
    
    # ==================== 音色管理接口 ====================
    
    def test_get_voices(self):
        """测试：获取音色列表"""
        self.log("=" * 60)
        self.log("测试：获取音色列表")
        self.log("=" * 60)
        
        params = {"is_recommended": 1, "language": "zh-CN"}
        result = self.request("GET", "/voices", params=params)
        if result and result.get("code") == 10200:
            voices = result.get("data", {}).get("list", [])
            if voices:
                test_data["voice_id"] = voices[0].get("voice_id")
                self.log(f"获取音色列表成功，默认使用: {test_data['voice_id']}")
            return True
        return False
    
    # ==================== 断句标准接口 ====================
    
    def test_get_breaking_standards(self):
        """测试：获取断句标准"""
        self.log("=" * 60)
        self.log("测试：获取断句标准")
        self.log("=" * 60)
        
        result = self.request("GET", "/sentence-breaking/standards")
        return result is not None
    
    def test_save_breaking_settings(self):
        """测试：保存断句标准设置"""
        if not test_data.get("task_id"):
            self.log("跳过：task_id不存在", "WARN")
            return False
        
        self.log("=" * 60)
        self.log("测试：保存断句标准设置")
        self.log("=" * 60)
        
        params = {"taskid": test_data["task_id"]}
        data = {
            "breaking_standard_id": 2,
            "char_count": 50
        }
        result = self.request("POST", "/sentence-breaking/settings", params=params, data=data)
        return result is not None
    
    # ==================== 测试运行 ====================
    
    def run_all_tests(self):
        """运行所有测试"""
        self.log("=" * 80)
        self.log("开始运行接口测试")
        self.log("=" * 80)
        self.log(f"Base URL: {self.base_url}")
        self.log("")
        
        test_results = []
        
        # 任务管理
        test_results.append(("创建任务", self.test_create_task()))
        test_results.append(("获取任务详情", self.test_get_task_detail()))
        test_results.append(("获取任务列表", self.test_get_task_list()))
        
        # 句子管理
        test_results.append(("获取句子列表", self.test_get_sentence_list()))
        test_results.append(("获取句子详情", self.test_get_sentence_detail()))
        
        # 音色管理（先获取音色，后续测试会用到）
        test_results.append(("获取音色列表", self.test_get_voices()))
        
        # 语音合成
        test_results.append(("合成单个句子", self.test_synthesize_sentence()))
        test_results.append(("查询合成状态", self.test_get_synthesis_status()))
        
        # 参数调整
        test_results.append(("调整参数", self.test_adjust_parameters()))
        test_results.append(("调整参数（插入句子）", self.test_adjust_parameters_with_insert()))
        test_results.append(("调整参数（重新断句）", self.test_adjust_parameters_with_rebreak()))
        
        # 阅读规范
        test_results.append(("创建阅读规范", self.test_create_reading_rule()))
        test_results.append(("获取阅读规范列表", self.test_get_reading_rules()))
        test_results.append(("应用阅读规范", self.test_apply_reading_rule()))
        
        # 音频编辑
        test_results.append(("合并音频", self.test_merge_audio()))
        test_results.append(("查询合并状态", self.test_get_merge_status()))
        
        # 断句标准
        test_results.append(("获取断句标准", self.test_get_breaking_standards()))
        test_results.append(("保存断句标准设置", self.test_save_breaking_settings()))
        
        # 批量合成
        test_results.append(("批量合成", self.test_batch_synthesize()))
        test_results.append(("重新合成", self.test_resynthesize_sentence()))
        
        # 删除句子（最后执行，避免影响其他测试）
        # test_results.append(("删除句子", self.test_delete_sentence()))
        
        # 输出测试结果
        self.log("")
        self.log("=" * 80)
        self.log("测试结果汇总")
        self.log("=" * 80)
        
        passed = sum(1 for _, result in test_results if result)
        total = len(test_results)
        
        for test_name, result in test_results:
            status = "✓ 通过" if result else "✗ 失败"
            self.log(f"{status} - {test_name}")
        
        self.log("")
        self.log(f"总计: {total} 个测试")
        self.log(f"通过: {passed} 个")
        self.log(f"失败: {total - passed} 个")
        self.log(f"成功率: {passed/total*100:.1f}%")
        self.log("=" * 80)


def main():
    """主函数"""
    import sys
    
    # 支持命令行参数指定Base URL
    base_url = BASE_URL
    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    
    tester = APITester(base_url)
    tester.run_all_tests()


if __name__ == "__main__":
    main()

