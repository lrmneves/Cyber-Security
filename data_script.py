def update_instance(instance,used_attributes):
	"""update the line to have the new attributes
	"""
	instance = instance.split(",")
	if instance[-1] == "normal.":
		instance.append(0)
	else:
		instance.append(1)
	new_instance = []
	for i in used_attributes:
		new_instance.append(str(instance[i]))

	return ",".join(new_instance)

names_file = open("kddcupBackup.names.txt", "r")

attributes = names_file.readlines()
attributes = attributes[1:]

for i in range(len(attributes)):
	temp = attributes[i].split(":")
	attributes[i] = temp[0]
"""
	Using attributes:
		duration - 0
		protocol_type - 1
		service - 2
		src_bytes - 4
		dst_bytes - 5
		land - 6
		same_srv_rate - 28
		dst_host_same_srv_rate - 33
		attack_cat - 41
		label - 42

"""
used_attributes = [0,1,2,4,5,6,28,33,41,42]

kdd_data = open("kddcup.data_10_percent.csv","r")

kdd_instances = kdd_data.readlines()
#handle the case where data is not formatted properly
if len(kdd_instances) < 2:
	kdd_instances = kdd_instances[0].split("\r")

header = ""
for i in range(len(used_attributes) -2 ): #-2 because attibutes do not have attack_cat and label values
 		header+=str(attributes[used_attributes[i]])
 		header+=","


header+="attack_cat,label\n"

new_data_file = open("new_data.csv","w")
new_data_file.write(header)

for instance in kdd_instances:
	new_instance = update_instance(instance,used_attributes)
	new_data_file.write(new_instance+"\n")


# f = open("kddcup.data.csv","r")

# lines = f.readlines()
# if len(lines) < 2:
# 	lines = lines[0].split("\r")
# newLines = []
# for line in lines:
# 	editLine(line,newLines)


# print len(newLines)
# f.close() 
# f = open("newEditedData.csv","w")
# for newLine in newLines:
# 	f.write(newLine)
# f.close()

