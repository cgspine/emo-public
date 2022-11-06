
const QUEUE_MSG = "emo://__QUEUE_MSG__/"
const CMD_GET_SUPPORTED_LIST = "__getSupportedCmdList__"
const CMD_ON_BRIDGE_READY = "__onBridgeReady__"
 
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

    async function send(opts) {
        if(!opts.cmd) {
            throw new Error('cmd is undefined.')
        }
        let supported = await isCmdSupport(opts.cmd)
        if(!supported){
            throw new Error(`cmd(${opts.cmd}) is not supported`)
        }
        return await new Promise((resolve, reject)=> {
            var message = {
                cmd: opts.cmd,
                data: opts.data,
            }
            var responseId = nextResponseId()
            var holding  = { 
                onResponse: data => resolve(data),
                onFailed: error => reject(new Error(error))
            }
            responseHoldings[responseId] = holding
            message.responseId = responseId;
            if(typeof opts.timeout === 'number' && opts.timeout > 0){
                holding.timeoutId = setTimeout(() => {
                    delete responseHoldings[responseId]
                    reject(new Error(`cmd(${opts.cmd}) timed out`))
                }, opts.timeout);
            }
            sendingMessageQueue.push(message)
            messagingIframe.src = QUEUE_MSG
        })
        
    }

    async function isCmdSupport(cmd){
        if(cmd == CMD_GET_SUPPORTED_LIST || cmd == CMD_ON_BRIDGE_READY){
            return true
        }
        let list = await getSupportedCmdList()
        return list.indexOf(cmd) >= 0
    }


    async function getSupportedCmdList(){
        if(getSupportedCmdList.__cache){
            return getSupportedCmdList.__cache
        }
        
        let data = await send({
            cmd: CMD_GET_SUPPORTED_LIST
        })
        getSupportedCmdList.__cache = data
        return data
    }

    function _fetchQueueFromNative(){
        var ret = sendingMessageQueue
        sendingMessageQueue = [];
        return ret;
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