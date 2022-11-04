
var QUEUE_HAS_MSG = "emo://__QUEUE_MSG__/"
 
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

    function nextCallbackId(){
        return `cb_${uuid++}`
    }

    function send(data, callback, timeout, timeoutCallback) {
        if(!data){
            throw new Error("message == null")
        }
        var message = {
            data: data
        }
        if(callback){
            var callbackId = nextCallbackId()
            var holding  = { 
                callback: callback
            };
            responseHoldings[callbackId] = holding
            message.callbackId = callbackId;
            if(typeof timeout === 'number' && timeout > 0){
                holding.timeoutId = setTimeout(function(){
                    delete responseHoldings[callbackId]
                    if(typeof timeoutCallback === 'function'){
                        timeoutCallback()
                    }
                }, timeout);
            }
        }
        sendingMessageQueue.push(message)
        messagingIframe.src = QUEUE_HAS_MSG
    }

    function isCmdSupport(cmd, callback){
        getSupportedCmdList(function(data){
            callback(data.indexOf(cmd))
        })
    }


    function getSupportedCmdList(callback){
        if(getSupportedCmdList.__cache){
            callback(getSupportedCmdList.__cache)
            return
        }
        send({__cmd__: "getSupportedCmdList"}, function(data){
            getSupportedCmdList.__cache = data
            callback(data)
        })
    }

    function _fetchQueueFromNative(){
        var messageQueueString = JSON.stringify(sendingMessageQueue)
        sendingMessageQueue = [];
        return messageQueueString;
    }

    function _handleResponseFromNative(response){
        if(response && response.callbackId){
            var holding = responseHoldings[response.callbackId]
            if(holding){
                holding.timeoutId && clearTimeout(holding.timeoutId)
                holding.callback(response.data)
                delete responseHoldings[response.callbackId]
            }
        }
    }
    

    var bridge = {
        send: send,
        isCmdSupport: isCmdSupport,
        getSupportedCmdList: getSupportedCmdList,
        _fetchQueueFromNative: _fetchQueueFromNative,
        _handleResponseFromNative: _handleResponseFromNative
    }

    var readyEvent = new Event('EmoBridgeReady')
    readyEvent.bridge = bridge
    doc.dispatchEvent(readyEvent)
    return bridge
 }
 

window.EmoBridge || (window.EmoBridge = createBridge(document))