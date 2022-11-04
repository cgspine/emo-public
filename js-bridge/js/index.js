
var QUEUE_MSG = "emo://__QUEUE_MSG__/"
var CMD_GET_SUPPORTED_LIST = "__getSupportedCmdList__"
var CMD_ON_BRIDGE_READY = "__onBridgeReady__"
 
function createIframe(doc) {
    var iframe = doc.createElement('iframe');
    iframe.style.display = 'none';
    doc.documentElement.appendChild(iframe);
    return iframe;
}


function createBridge(doc){
    var messagingIframe = createIframe(doc)
    var responseHoldings = {}
    var uuid = 1
    var sendingMessageQueue = []

    function nextResponseId(){
        return `cb_${uuid++}`
    }

    function send(opts) {
        if(!opts.cmd) {
            opts.onFailed && opts.onFailed("cmd is undefined.")
            return
        }

        isCmdSupport(opts.cmd, function(supported) {
            if(!supported) {
                opts.onFailed && opts.onFailed("cmd is not supported.")
                return
            }
            var message = {
                cmd: opts.cmd,
                data: opts.data,
            }
            if(typeof opts.onResponse === "function"){
                var responseId = nextResponseId()
                var holding  = { 
                    onResponse: opts.onResponse,
                    onFailed: opts.onFailed
                }
                var onTimeout = opts.onTimeout
                responseHoldings[responseId] = holding
                message.responseId = responseId;
                if(typeof opts.timeout === 'number' && opts.timeout > 0){
                    holding.timeoutId = setTimeout(function(){
                        delete responseHoldings[responseId]
                        if(typeof onTimeout === 'function'){
                            onTimeout()
                        }
                    }, opts.timeout);
                }
            }
            sendingMessageQueue.push(message)
            messagingIframe.src = QUEUE_MSG
        })
    }

    function isCmdSupport(cmd, callback){
        if(cmd == CMD_GET_SUPPORTED_LIST || cmd == CMD_ON_BRIDGE_READY){
            callback(true)
            return
        }
        getSupportedCmdList(function(data){
            callback(data.indexOf(cmd) >= 0)
        })
    }


    function getSupportedCmdList(callback){
        if(getSupportedCmdList.__cache){
            callback(getSupportedCmdList.__cache)
            return
        }
        send({
            cmd: CMD_GET_SUPPORTED_LIST,
            onResponse: function(data){
                getSupportedCmdList.__cache = data
                callback(data)
            }
        })
    }

    function _fetchQueueFromNative(){
        var messageQueueString = JSON.stringify(sendingMessageQueue)
        sendingMessageQueue = [];
        return messageQueueString;
    }

    function _handleResponseFromNative(response){
        if(response && response.responseId){
            var holding = responseHoldings[response.responseId]
            if(holding){
                holding.timeoutId && clearTimeout(holding.timeoutId)
                if(response.error){
                    holding.onFailed && holding.onFailed(response.error)
                }else{
                    holding.onResponse(response.data)
                }
                delete responseHoldings[response.responseId]
            }
        }
    }
    

    return {
        send: send,
        isCmdSupport: isCmdSupport,
        getSupportedCmdList: getSupportedCmdList,
        _fetchQueueFromNative: _fetchQueueFromNative,
        _handleResponseFromNative: _handleResponseFromNative
    }
 }
 
if(!window.EmoBridge){
    var bridge = window.EmoBridge = createBridge(document)
    bridge.send({
        cmd: CMD_ON_BRIDGE_READY
    })
    var readyEvent = new Event('EmoBridgeReady')
    readyEvent.bridge = bridge
    document.dispatchEvent(readyEvent)
}