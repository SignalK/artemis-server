const path = require('path');

module.exports = {
	entry : './parser.js',
	output : {
		library : 'parser',
		libraryTarget : 'var',
		filename : 'bundle.js',
		path : path.resolve(__dirname, 'dist')
	},
	node : {
		fs : 'empty'
	}
};