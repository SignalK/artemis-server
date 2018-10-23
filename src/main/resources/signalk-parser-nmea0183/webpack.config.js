const path = require('path');

module.exports = {
		module: {
		    loaders: [
		        {include: /\.json$/, loaders: ["json-loader"]}
		    ]
		  },
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