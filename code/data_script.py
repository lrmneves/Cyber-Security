def update_instance(instance,used_attributes,attack_types_map,attr_dict = None,divide_set = None):
	"""update the line to have the new attributes
	"""
	instance = instance.split(",")
	if instance[-1][-1] == '\n':
		instance[-1]= instance[-1][:-1]
	if instance[-1][-1] == '.':
		instance[-1]= instance[-1][:-1]
	if(attr_dict == None):
		if instance[-1] == "normal":
			instance.append(0)
		else:
			instance[-1] = attack_types_map[instance[-1]]
			instance.append(1)


		new_instance = []
		for i in used_attributes:
			new_instance.append(str(instance[i]))
	else:
		new_instance = []
		for i in used_attributes:
			if divide_set != None and i in divide_set:
				new_instance.append(str(convertRate(instance[attr_dict[i]])))
			else:
				if(i == used_attributes[len(used_attributes)-2]):
					new_instance.append(attack_types_map[instance[attr_dict[i]]])
				else:
					new_instance.append(str(instance[attr_dict[i]]))

	return ",".join(new_instance)

def convertRate(rate):
	return float(rate)/100



"""
	map of attributes:
		duration - 0 - 6
		protocol_type - 1 - 4
		service - 2 - 13
		src_bytes - 4 - 7
		dst_bytes - 5 - 8
		land - 6 - 35
		same_srv_rate - 28 40 #have to divide value by 100 to convert
		dst_host_same_srv_rate - 33 41 #have to divide value by 100 to convert
		attack_cat - 41
		label - 42

"""
attributes_dict ={}

attributes_dict[0] = 6
attributes_dict[1] = 4
attributes_dict[2] = 13
attributes_dict[4] = 7
attributes_dict[5] = 8
attributes_dict[6] = 35
attributes_dict[28] = 40
attributes_dict[33] = 41
attributes_dict[41] = 43
attributes_dict[42] = 44
#index of values which should be converted
divide_set = set([28,33])
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

"""

	Map of Attacks to categories

		back dos
		buffer_overflow u2r
		ftp_write r2l
		guess_passwd r2l
		imap r2l
		ipsweep probe
		land dos
		loadmodule u2r
		multihop r2l
		neptune dos
		nmap probe
		perl u2r
		phf r2l
		pod dos
		portsweep probe
		rootkit u2r
		satan probe
		smurf dos
		spy r2l
		teardrop dos
		warezclient r2l
		warezmaster r2l
		#From second dataset
		Reconnaissance 	probe
		DoS	dos
		Exploit	r2l
		Analysis	probe
		Fuzzers	dos
		Worms	r2l
		Generic	
		Shellcode	u2r
		Backdoors	r2l

"""
attack_types_map ={}
attack_names_data = open("../data/training_attack_types.txt","r")

attack_names = attack_names_data.readlines()
for attack in attack_names:
	key_value = attack.split(" ")
	if(len(key_value) > 1):
		attack_types_map[key_value[0]] = key_value[1][:-1]
attack_names_data.close()

attack_types_map["Reconnaissance"] = "probe"
attack_types_map["DoS"] = "dos"
attack_types_map["Exploits"] = "r2l"
attack_types_map["Analysis"] = "probe"
attack_types_map["Fuzzers"] = "dos"
attack_types_map["Worms"] = "r2l"
attack_types_map["Generic"] = "generic"
attack_types_map["Shellcode"] = "u2r"
attack_types_map["Backdoor"] = "r2l"
attack_types_map["Normal"] = "normal"

#Loading the attribute names

names_file = open("../data/kddcup.names.txt", "r")

attributes = names_file.readlines()
attributes = attributes[1:]

for i in range(len(attributes)):
	temp = attributes[i].split(":")
	attributes[i] = temp[0]

names_file.close()


kdd_data = open("../data/kddcup.data_10_percent.csv","r")

kdd_instances = kdd_data.readlines()
#handle the case where data is not formatted properly
if len(kdd_instances) < 2:
	kdd_instances = kdd_instances[0].split("\r")

header = ""
for i in range(len(used_attributes) -2 ): #-2 because attibutes do not have attack_cat and label values
 		header+=str(attributes[used_attributes[i]])
 		header+=","

header+="attack_cat,label\n"

new_data_file = open("../output/new_data.csv","w")
new_data_file.write(header)

for instance in kdd_instances:
	new_instance = update_instance(instance,used_attributes,attack_types_map)
	new_data_file.write(new_instance+"\n")

kdd_data.close()

unsw_data = open("../data/UNSW_NB15_training-set.csv")

unsw_instances =unsw_data.readlines()
unsw_instances = unsw_instances[1:] #removes header


for instance in unsw_instances:
	new_instance = update_instance(instance,used_attributes,attack_types_map,attributes_dict,divide_set)
	new_data_file.write(new_instance)

unsw_data.close()
new_data_file.close()

