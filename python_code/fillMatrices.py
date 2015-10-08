from pandas import DataFrame as df
import pandas as pd 
import numpy as np 
import matplotlib.pyplot as plt
from sklearn.linear_model import LinearRegression
from scipy.stats import zscore
def normalize(data):
	norm = data
	for col in norm:
		if not col == "attack_cat" and  (data[col].max() - data[col].min() !=0):
			data[col] = (data[col] - data[col].min()) / (data[col].max() - data[col].min())
	
	# norm = (data- data.mean())/data.std(ddof=0)

	return norm

complete_data  = df.from_csv("../output/complete_data.csv",header=0  ,sep=',', index_col=False)
missing_data  = df.from_csv("../output/missing_values_data.csv",header=0  ,sep=',', index_col=False)
# final_data = df.from_csv("../output/final_data.csv",header=0  ,sep=',', index_col=False)


columns = missing_data.iloc[:,1:].isnull().any()
good_columns = []
bad_columns = []
for i in range(len(columns)):
	if columns.iloc[i] == True:
		bad_columns.append(i)



for i in range(18):
	# lr = GaussianProcess(theta0=1e-4, nugget=1e-10);
	lr = LinearRegression()
    
	only_good_dataset = pd.concat([normalize(complete_data.iloc[:,1:23+i]),complete_data.iloc[:,43]], axis=1)
	only_bad_dataset = normalize(complete_data.iloc[:,24+i:42])
	X =np.asarray(only_good_dataset)
	y = np.asarray(only_bad_dataset.iloc[:,:1])



	lr.fit(X, y)

	missing_data_good_features = pd.concat([normalize(missing_data.iloc[:,1:23+i]),missing_data.iloc[:,43]], axis=1)


# print missing_data_good_features
	# newC,trash = lr.predict(missing_data_good_features, eval_MSE=True)
	newC= lr.predict(missing_data_good_features)


	missing_data.iloc[:,24+i] = newC
final_data = pd.concat([missing_data,complete_data])

msk = np.random.rand(len(final_data)) < 0.80
final_data=final_data[~msk]
msk = np.random.rand(len(final_data)) < 0.80
train_data = final_data[msk]
test_data = final_data[~msk]
train_data = normalize(train_data)
test_data = normalize(test_data)
print len(train_data)
print len(test_data)
#save to csv
train_data.to_csv("../output/train_data.csv")
test_data.to_csv("../output/test_data.csv")
