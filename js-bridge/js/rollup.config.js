import terser from '@rollup/plugin-terser';

export default {
    input: 'index.js',
    output: {
        file: '../src/main/assets/emo-bridge.js', 
        format: 'iife'
    },
    plugins: [terser()]
}