//********************************************************************************//
//		功能名称:	T16定时一定时间（上升沿计时）
//		功能编号:	080101
//		适应芯片:	适用于所有芯片
//--------------------------------------------------------------------------------//
//程序说明:
//	利用T16计数器来计时，每隔10ms计时一次，计时200次（两秒后）控制LED灯由亮变灭
//注意事项:
//	1.案例为PMS154C，其他芯片原理都相同
//********************************************************************************//
#include	"extern.h"
#include	"user.h"
// #include 	"peacock.h" 


//========版本信息========
//版本日期：2026-04-27 
//版本号：V1.0.1	
//版本描述：修改中版本
/*
未完成：复位

已完成：按键、定时器、pwm、串口、ADC、SPI+RF、模式震动、充电、低功耗
*/


//=======模式震动=========


// 200ms模式3,	50ms关闭
static void FourMode(void)
{
	if(4 == WorkMode)
	{
		if(0 == ModeSegFlag) //模式1
		{
			if(1 == PwmOpenFlag)
			{
				OpenPWMCntms++;
				if(OpenPWMCntms >= 5)  
				{
					OpenPWMCntms = 0;
					ModeSegFlag = 1;
					ModeSegOKFg = 1;

				}	
			}
		}
		else //ModeSegFlag = 1 //模式2
		{
			if(1 == PwmOpenFlag)
			{
				OpenPWMCntms++;
				if(OpenPWMCntms >= 20)
				{
					OpenPWMCntms = 0;
					ModeSegFlag = 0;
					ModeSegOKFg = 1;

				}	
			}
		}
		

	}
	
}

// 6段200ms(100ms开100ms关) + 3段400MS(300ms开100ms关)
static void FiveMode(void)
{
	if(5 == WorkMode)
	{
		if(0 == ModeSegFlag) //6段200ms(100ms开100ms关)
		{
			if(1 == PwmOpenFlag)
			{
				OpenPWMCntms++;
				if(OpenPWMCntms >= 10) //100 开
				{
					OpenPWMCntms = 0;
					PwmOpenFlag = 0;
					ModeSegOKFg = 1;

				}	
				
			}
			else
			{
				OpenPWMCntms++;
				if(OpenPWMCntms >= 10) //100 关
				{
					OpenPWMCntms = 0;
					PwmOpenFlag = 1;
					ModeSegOKFg = 1;
					ModeSegCnt++;

					if(ModeSegCnt >= 6)
					{
						ModeSegCnt = 0;
						ModeSegFlag = 1;
						ModeSegOKFg = 1;
						//6段结束 变为 3段400MS(300ms开100ms关)		
					}

				}	

			}
			

		}
		else //ModeSegFlag = 1  3段400MS(300ms开100ms关)
		{
			if(1 == PwmOpenFlag)
			{
				OpenPWMCntms++;
				if(OpenPWMCntms >= 30) //300ms 开
				{
					OpenPWMCntms = 0;
					PwmOpenFlag = 0;
					ModeSegOKFg = 1;
				}	
				
			}
			else
			{
				OpenPWMCntms++;
				if(OpenPWMCntms >= 10) //100ms 关
				{
					OpenPWMCntms = 0;
					PwmOpenFlag = 1;
					ModeSegOKFg = 1;
					ModeSegCnt++;
					if(ModeSegCnt >= 3)
					{
						ModeSegCnt = 0;
						ModeSegFlag = 0;
						ModeSegOKFg = 1;
					}

				}	

			}
		}
		
	}

	
}


// 400ms（200ms开200ms关）
static void SixMode(void)
{
	if(6 == WorkMode)
	{
		if(0 == ModeSegFlag) //模式1
		{
			if(1 == PwmOpenFlag)
			{
				OpenPWMCntms++;
				if(OpenPWMCntms >= 20)  
				{
					OpenPWMCntms = 0;
					ModeSegFlag = 1;
					ModeSegOKFg = 1;

				}	
			}
		}
		else //ModeSegFlag = 1 //模式2
		{
			if(1 == PwmOpenFlag)
			{
				OpenPWMCntms++;
				if(OpenPWMCntms >= 20)
				{
					OpenPWMCntms = 0;
					ModeSegFlag = 0;
					ModeSegOKFg = 1;

				}	
			}
		}
		

	}
	
}


// 占空比从15%-->100%（1.2s） 6ms 37-->255
static void SevenMode(void)
{

	if(7 == WorkMode)
	{
		if(1 == PwmOpenFlag)
		{
			OpenPWMCntms++;
			if(OpenPWMCntms >= 1)  
			{
				OpenPWMCntms = 0;
				PwmShockDutyCnt++;
				if(PwmShockDutyCnt >= 255)
				{
					PwmShockDutyCnt = 37;
				}
				ModeSegFlag = 1;
				ModeSegOKFg = 1;

			}	
		}

	}
	

}


//  2个200ms(一半占空比0.15(750ns)+一半高电平)+300（100ms占空比0.15(750ns)+200ms高电平）
static void EightMode(void)
{
	if(8 == WorkMode)
	{
		if(0 == ModeSegFlag) //模式1
		{
			if(1 == PwmOpenFlag)  //占空比0.15
			{
				OpenPWMCntms++;
				if(OpenPWMCntms >= 10)  
				{
					OpenPWMCntms = 0;
					ModeSegOKFg = 1;
					PwmOpenFlag = 0;
				}	
			}
			else		 //高电平
			{
				OpenPWMCntms++;
				if(OpenPWMCntms >= 10)  
				{
					OpenPWMCntms = 0;
					ModeSegOKFg = 1;
					PwmOpenFlag = 1;
					ModeSegCnt++;
					if(ModeSegCnt >= 2)
					{
						ModeSegCnt = 0;
						ModeSegFlag = 1;
						ModeSegOKFg = 1;
					}		
				}	

			}

		}
		else //ModeSegFlag = 1 //模式2
		{
			if(1 == PwmOpenFlag)  //100ms占空比0.15
			{
				OpenPWMCntms++;
				if(OpenPWMCntms >= 10)  
				{
					OpenPWMCntms = 0;
					ModeSegOKFg = 1;
					PwmOpenFlag = 0;
				}	
			}
			else		 //200ms高电平
			{
				OpenPWMCntms++;
				if(OpenPWMCntms >= 20)  
				{
					OpenPWMCntms = 0;
					ModeSegOKFg = 1;
					PwmOpenFlag = 1;
					ModeSegFlag = 0;
				}	

			}
		}
		

	}

}


// 1s占空比0.15(750ns)+1.5s高电平
static void NineMode(void)
{
	if(9 == WorkMode)
	{
		if(0 == ModeSegFlag) //模式1
		{
			if(1 == PwmOpenFlag)  //占空比0.15
			{
				OpenPWMCntms++;
				if(OpenPWMCntms >= 100)  
				{
					OpenPWMCntms = 0;
					ModeSegOKFg = 1;
					ModeSegFlag = 1;
				}	
			}

		}
		else //ModeSegFlag = 1 //模式2
		{
			if(1 == PwmOpenFlag)  //100ms占空比0.15
			{
				OpenPWMCntms++;
				if(OpenPWMCntms >= 150)  
				{
					OpenPWMCntms = 0;
					ModeSegOKFg = 1;
					ModeSegFlag = 0;
				}	
			}

		}
		

	}

}


void JudgeMode_Timer(void)
{
	//======User can add code=====
	//判断模式震动标志位
	//============================


	if(	(WorkMode != 0) &&
		(WorkMode != 0x0A) &&
		(ChargingStatus == 0))
	{
		FourMode();
		FiveMode();
		SixMode();
		SevenMode();
		EightMode();
		NineMode();
	}
	else
	{
		ModeSegOKFg = 1;
	}


}



//==========UART======================

UART_Clock		=>	8000000;			//UART时钟，选择1M、2M、4M、8M，其他值默认1M，若使用其他值请咨询FAE
FPPA_Duty		=>	_SYS(INC.FPPA_NUM);	// Single FPPA = 1, Mult FPPA = 2 or 4/8/...

Baud_Rate		=>	38400;				//波特率

/*
UART_Delay		=>	(UART_Clock / FPPA_Duty) / Baud_Rate;

	if	UART_Clock	=	8,000.000 Hz
		FPPA_Duty		=	/16
	so	FPPA_Clock		=	UART_Clock / FPPA_Duty	=	500.000	Hz

	if	Baud_Rate		=	19200
	so	UART_Delay		=	500.000 / 19200 = 26.0416...
	so	match, 26 cycles send one bit.	< 0.5%

	if	Baud_Rate		=	38400
	so	UART_Delay		=	500.000 / 38400 = 13.02083...
	so	match, 13 cycles send one bit.	< 0.5%

	if	Baud_Rate		=	56000
?	so	UART_Delay		=	500.000 / 56000 = 8.9285...		<	1.0%

	if	Baud_Rate		=	57600
X	so	UART_Delay		=	500.000 / 57600 = 8.6805...		:	fail
*/
UART_Delay		=>	( (UART_Clock / FPPA_Duty) + (Baud_Rate/2) ) / Baud_Rate;
//	+ (Baud_Rate/2) : to round up or down
Test_V0			=>	UART_Clock / 1000 * 995;
Test_V1			=>	UART_Delay * Baud_Rate * FPPA_Duty;
Test_V2			=>	UART_Clock / 1000 * 1005;

#if	(Test_V1 < Test_V0) || (Test_V1 > Test_V2)
	.echo	%Test_V0 <= %Test_V1 <= %Test_V2
	.error	Baud_Rate do not match to System Clock
#endif


byte	SYS_CLKMD;
byte	CLKMD_BK;

void	Clock_Adjust(void)//时钟调整
{
	#if		UART_Clock == 8000000
	{
		CLKMD_BK	=	0x34;//8M
	}
	#elseif	UART_Clock == 4000000
	{
		CLKMD_BK	=	0x14;//4M
	}
	#elseif	UART_Clock == 2000000
	{
		CLKMD_BK	=	0x3c;//2M
	}
	#else
	{
		UART_Clock	=>	1000000
		CLKMD_BK	=	0x1c;//1M
	}
	#endif
	CLKMD = CLKMD_BK;//将系统时钟修改为设定的UART时钟
	nop;//等待
}


//发送程序
static	void	UART_Send (byte uartsendbyte)
{
	BYTE	cnt;
	BYTE	UART_Data_Out;

	Disgint;

	UART_Data_Out = uartsendbyte;

	//	Start Bit
	set0	UART_Out;				//	1

	#if	FPPA_Duty == 1
		cnt	=	8;						//	2 ~ 3
		.Delay	3;						//	4 ~ 6
		do
		{	//	Data Bit * 8
			.Delay	UART_Delay - 10;
			sr		UART_Data_Out;		//	7
			if (CF)
			{
				nop;					//	10
				UART_Out	=	1;		//	1
			}
			else
			{
				UART_Out	=	0;		//	1
				.delay	2;				//	2 ~ 3
			}
		} while (--cnt);				//	4 ~ 6
		.Delay	UART_Delay - 5;
	#else
		.Delay	UART_Delay - 4;
		cnt	=	8;						//	2 ~ 3

		//	Data Bit * 8
		do
		{
			sr		UART_Data_Out;		//	4		4
			swapc	UART_Out;			//			1
			.Delay	UART_Delay - 4;
		} while (--cnt);				//			2, 3

		.Delay	2;						//			3 ~ 4
	#endif

	//	Stop Bit
	set1	UART_Out;				//	1
	.Delay	2 * UART_Delay - 2;

	Engint;
}									//	2


void	UART_HandShake (byte reg,byte *buff, byte len)
{
	Clock_Adjust();		//将系统时钟修改为设定的UART时钟

	UART_Send(reg);		//发送数据

	bit 	SendOverFlag = 0;
	byte	UartData;
	word	pnt_SPI_Out;
	word	pnt_SPI_Put;
	pnt_SPI_Out = buff;//第一个数据的地址
	pnt_SPI_Put = buff + len-1;//最后一个数据的地址

	do
	{
		UartData = *pnt_SPI_Out;
		UART_Send(UartData);//将pnt_SPI_Out地址上的值发给从机
		pnt_SPI_Out++;
		if(pnt_SPI_Out > pnt_SPI_Put)
		{
			pnt_SPI_Out = buff;//10组发送完毕，再重新发送10组
			SendOverFlag = 1;
		}
	} while (!SendOverFlag);

	// UART_Send(0xAA);		//发送数据
	// UART_Send(0x55);		//发送数据
	// UART_Send(0x66);		//发送数据
	// UART_Send(0x77);		//发送数据
	// UART_Send(0x88);		//发送数据

	CLKMD = SYS_CLKMD;	//数据发送结束后，切回原来的系统时钟
	nop;//等待


}


void	UART_Send_Data(byte reg,byte data)
{
	Clock_Adjust();		//将系统时钟修改为设定的UART时钟

	UART_Send(reg);		//发送数据
	UART_Send(data);		//发送数据

	// UART_Send(0xAA);		//发送数据
	// UART_Send(0x55);		//发送数据
	// UART_Send(0x66);		//发送数据
	// UART_Send(0x77);		//发送数据
	// UART_Send(0x88);		//发送数据

	CLKMD = SYS_CLKMD;	//数据发送结束后，切回原来的系统时钟
	nop;//等待

}


void	UART_Send_Byte(byte data)
{
	Clock_Adjust();		//将系统时钟修改为设定的UART时钟

	UART_Send(data);		//发送数据

	// UART_Send(0xAA);		//发送数据
	// UART_Send(0x55);		//发送数据
	// UART_Send(0x66);		//发送数据
	// UART_Send(0x77);		//发送数据
	// UART_Send(0x88);		//发送数据

	CLKMD = SYS_CLKMD;	//数据发送结束后，切回原来的系统时钟
	nop;//等待

}



//=======SPI+RF=========


#if 1

void rf_spi_init( void )
{
	$ SPI_CSN out,high;//
	$ SPI_CLK out,low;//
	$ SPI_DATA in,pull;//

}

/**
 * @brief       Spi of RF read data
 * @param       void
 * @retval      uint8_t - data
 */
byte rf_spi_read( void )
{
    byte data = 0;
	byte cnt = 8;

	$ SPI_DATA in;//
	
	do
	{
		$ SPI_CLk Out,Low;	// 时钟拉低（准备产生一个时钟周期）
		      
        data = data << 1;	// 先左移1位，给即将读入的bit腾位置
       
        $ SPI_CLk Out,High;;	 // 时钟拉高，从机在此时刻输出数据（或主机在此采样）
        
        if(SPI_DATA)	// 读取SDO引脚状态（读取当前bit）
            data |= 0x01;   // 如果为1，最低位赋1

	}while(--cnt);

    $ SPI_CLk Out,Low;	// 通信结束，时钟拉低

	return data;   // 返回读取到的1字节数据

}


//******************************************************
//读数据
//******************************************************
void SPI_DataRead(void)  
{
	SPIRData=0;
	SPICnt=8;
	do
	{
		$ SPI_CLk Out,Low;	// 时钟拉低（准备产生一个时钟周期）
		.delay 10;
		SPIRData<<=1;
        $ SPI_CLk Out,High;;	 // 时钟拉高，从机在此时刻输出数据（或主机在此采样）
		.delay 10;
		if(SPI_DATA)
		{
			SPIRData |=0x01;
		}
	}while(--SPICnt);

    $ SPI_CLk Out,Low;	// 通信结束，时钟拉低
}



/**
 * @brief       Spi of RF write data
 * @param       uint8_t - data
 * @retval      void
 */
void rf_spi_write(byte write_data)
{
		
	byte cnt = 8;

	do
	{
		$ SPI_CLk Out,Low;//主机时钟输出低，发送数据，从机等待
		if(write_data & 0x80)
		{
			$ SPI_DATA Out,High;
		}
		else
		{
			$ SPI_DATA Out,Low;
		}
		$ SPI_CLk Out,High;//主机时钟输出高，从机开始接收数据
		write_data <<= 1;
	}while(--cnt);

	$ SPI_DATA in,pull;//
	$ SPI_CLk Out,Low;//主机时钟输出低，发送数据，从机等待

}

//******************************************************
//写单个字节数据
//******************************************************
void SPI_DataWrite(void)   
{
	SPICnt=8;
	do
	{
		$ SPI_CLk Out,Low;//主机时钟输出低，发送数据，从机等待
		// .delay 10;
		if(SPIWData&0x80)
		{
			$ SPI_DATA Out,High;
		}
		else 
		{
			$ SPI_DATA Out,Low;
		}
		SPIWData<<=1;
		$ SPI_CLk Out,High;//主机时钟输出高，从机开始接收数据
		// .delay 10;
	}while(--SPICnt);

	$ SPI_DATA in,pull;//
	$ SPI_CLk Out,Low;//主机时钟输出低，发送数据，从机等待
	SPIWData=0;
}


/**
 * @brief       Spi of RF read buffer
 * @param       uint8_t - reg,
 *              uint8_t* - buff,
 *              uint8_t - len
 * @retval      uint8_t - buff[0]
 */
byte rf_spi_read_reg(byte reg)
{
	byte buff;
	$ SPI_CSN out,low;//
    rf_spi_write(reg);
	buff = rf_spi_read();
    $ SPI_CSN out,high;//

    return buff;
}

//******************************************************
//读寄存器值
//******************************************************
void RF_ReadReg(void)
{
	$ SPI_CSN out,low;//
	SPIWData=RFAddr;
	SPI_DataWrite();   //发送一个要读的地址数据
	SPI_DataRead();   //读回该地址的数据
    $ SPI_CSN out,high;//
	RFRData=SPIRData;
}


/**
 * @brief       Spi of RF read buffer
 * @param       uint8_t - reg,
 *              uint8_t* - buff,
 *              uint8_t - len
 * @retval      uint8_t - buff[0]
 */
// void rf_spi_read_buff_rx1(byte reg,  byte len)
// {

// 	word Data_Point; 

// 	$ SPI_CSN out,low;//
//     rf_spi_write(reg);

// 	if(len == 0)
// 	{
// 		$ SPI_CSN out,high;

// 		return;
// 	}

// 	Data_Point = &rx_buff[0];

// 	do
// 	{

// 		*Data_Point = rf_spi_read();//
// 		Data_Point++;

// 	} while (--len);

// 	$ SPI_CSN out,high;



// }


/**
 * @brief       Spi of RF read buffer
 * @param       uint8_t - reg,
 *              uint8_t* - buff,
 *              uint8_t - len
 * @retval      uint8_t - buff[0]
 */
byte rf_spi_read_buff(byte reg, byte *buff, byte len)
{
	$ SPI_CSN out,low;//
    rf_spi_write(reg);

	if(len)
	{
		// for(uint8_t i = 0; i < len; i++)
		//     buff[i] = rf_spi_read( );
		bit 	ReceiveOverFlag = 0;
		word	pnt_SPI_In;
		word	pnt_SPI_Get;

		pnt_SPI_In = buff;//第一个数据的地址
		pnt_SPI_Get = buff + len;//最后一个数据的地址

		do
		{
			*pnt_SPI_In = rf_spi_read();//将读取到的数据存入pnt_SPI_In地址
			pnt_SPI_In++;
			if(pnt_SPI_In == pnt_SPI_Get)//10组数据传输完毕
			{
				// pnt_SPI_In = buff;//地址恢复，准备重新传输10组数据
				ReceiveOverFlag = 1;

			}

		} while (!ReceiveOverFlag);

	}


    $ SPI_CSN out,high;//

    return *buff;
}

//-----------------------------------
void RF_ReadBuf(void)
{
	$ SPI_CSN out,low;//
	SPIWData=RFAddr;
	SPI_DataWrite();
	if((DataLen == 0) || (DataLen > BUFF_LEN))
	{
		$ SPI_CSN out,high;//
		return;
	}
	Data_Point=&RXbufData[0];
	do
	{
		SPI_DataRead(); 
		*Data_Point++ =SPIRData;
	}
	while(--DataLen);
    $ SPI_CSN out,high;//

}




// void rf_spi_read_buff_rx2(byte reg, byte len)
// {
// 	$ SPI_CSN out,low;//
//     rf_spi_write(reg);

// 	if(len)
// 	{
// 		// for(uint8_t i = 0; i < len; i++)
// 		//     buff[i] = rf_spi_read( );
// 		bit 	ReceiveOverFlag = 0;
// 		word	pnt_SPI_In;
// 		word	pnt_SPI_Get;

// 		pnt_SPI_In = rx_buff;//第一个数据的地址
// 		pnt_SPI_Get = rx_buff + len;//最后一个数据的地址

// 		do
// 		{
// 			*pnt_SPI_In = rf_spi_read();//将读取到的数据存入pnt_SPI_In地址
// 			pnt_SPI_In++;
// 			if(pnt_SPI_In == pnt_SPI_Get)//10组数据传输完毕
// 			{
// 				ReceiveOverFlag = 1;

// 			}

// 		} while (!ReceiveOverFlag);

// 	}


//     $ SPI_CSN out,high;//

//     // return *rx_buff;
// }

/**
 * @brief       Spi of RF write buffer
 * @param       uint8_t - reg,
 *              uint8_t* - buff,
 *              uint8_t - len
 * @retval      void
 */
void rf_spi_write_Reg(byte reg, byte buff)
{
	$ SPI_CSN out,low;//
    rf_spi_write(reg);
    rf_spi_write(buff);
	$ SPI_CSN out,high;//

}


//******************************************************
//写一个byte寄存器
//******************************************************
void RF_WriteReg(void)
{
	$ SPI_CSN out,low;//
	SPIWData=RFAddr;
	SPI_DataWrite();
	SPIWData=RFWData;
	SPI_DataWrite();
	$ SPI_CSN out,high;//
}

/**
 * @brief       Spi of RF write buffer
 * @param       uint8_t - reg,
 *              uint8_t* - buff,
 *              uint8_t - len
 * @retval      void
 */
void rf_spi_write_buff(byte reg, byte *buff, byte len)
{

	$ SPI_CSN out,low;//
    rf_spi_write(reg);

	if(len)
	{
		bit 	SendOverFlag = 0;
		byte	SpiData;
		word	pnt_SPI_Out;
		word	pnt_SPI_Put;
		pnt_SPI_Out = buff;//第一个数据的地址
		pnt_SPI_Put = buff + len-1;//最后一个数据的地址

		do
		{
			SpiData = *pnt_SPI_Out;
			rf_spi_write(SpiData);//将pnt_SPI_Out地址上的值发给从机
			pnt_SPI_Out++;
			if(pnt_SPI_Out > pnt_SPI_Put)
			{
				SendOverFlag = 1;
			}
		} while (!SendOverFlag);
		
	}

	$ SPI_CSN out,high;//

}

//******************************************************
//向寄存器写多个字节数据
//******************************************************
void RF_WriteBuf(void)
{
	$ SPI_CSN out,low;//
	SPIWData=RFAddr;
	SPI_DataWrite();
	Data_Point=&TXbufData[0];
	do
	{
		SPIWData = *Data_Point++;
		SPI_DataWrite();
	}
	while(--DataLen);
	$ SPI_CSN out,high;//

}



void RF_ReadReg_Debug(void)
{
    // uint8_t data[8];

    // rf_spi_read_buff(CFG_TOP, data, 4);
    // UART_HandShake(CFG_TOP, data, 5);  // 4+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(EN_AA, data, 1);
    // UART_HandShake(EN_AA, data, 2);    // 1+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(EN_RXADDR, data, 1);
    // UART_HandShake(EN_RXADDR, data, 2);  // 1+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(SETUP_AW, data, 1);
    // UART_HandShake(SETUP_AW, data, 2);    // 1+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(SETUP_RETR, data, 4);
    // UART_HandShake(SETUP_RETR, data, 5);  // 4+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(RF_CH, data, 4);
    // UART_HandShake(RF_CH, data, 5);       // 4+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(SETUP_RF, data, 4);
    // UART_HandShake(SETUP_RF, data, 5);    // 4+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(STATUS, data, 1);
    // UART_HandShake(STATUS, data, 2);     // 1+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(OBSERVE_TX, data, 4);
    // UART_HandShake(OBSERVE_TX, data, 5);  // 4+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(RX_ADDR_P0, data, 5);
    // UART_HandShake(RX_ADDR_P0, data, 6);  // 5+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(RX_ADDR_P1, data, 5);
    // UART_HandShake(RX_ADDR_P1, data, 6);  // 5+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(RX_ADDR_P2_TOP5, data, 5);
    // UART_HandShake(RX_ADDR_P2_TOP5, data, 6);  // 5+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(AGC_SETTING, data, 4);
    // UART_HandShake(AGC_SETTING, data, 5);  // 4+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(PGA_SETTING, data, 5);
    // UART_HandShake(PGA_SETTING, data, 6);  // 5+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(TX_ADDR, data, 5);
    // UART_HandShake(TX_ADDR, data, 6);      // 5+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(RX_PW_PX, data, 6);
    // UART_HandShake(RX_PW_PX, data, 7);    // 6+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(STATUS_FIFO, data, 4);
    // UART_HandShake(STATUS_FIFO, data, 5);  // 4+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(TXPROC_CFG, data, 4);
    // UART_HandShake(TXPROC_CFG, data, 5);   // 4+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(RXPROC_CFG, data, 5);
    // UART_HandShake(RXPROC_CFG, data, 6);   // 5+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(DYNPD, data, 1);
    // UART_HandShake(DYNPD, data, 2);        // 1+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(FEATURE, data, 4);
    // UART_HandShake(FEATURE, data, 5);      // 4+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(ANALOG_CFG1, data, 2);
    // UART_HandShake(ANALOG_CFG1, data, 3);  // 2+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(ANALOG_CFG2, data, 6);
    // UART_HandShake(ANALOG_CFG2, data, 7);  // 6+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(CFG_TOP, data, 1);
    // UART_HandShake(CFG_TOP, data, 2);      // 1+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(ANALOG_CFG3, data, 1);
    // UART_HandShake(ANALOG_CFG3, data, 2);  // 1+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(AGC_SETTING, data, 4);
    // UART_HandShake(AGC_SETTING, data, 5);  // 4+1
	// .delay	2000;		//等待

    // rf_spi_read_buff(PGA_SETTING, data, 5);
    // UART_HandShake(PGA_SETTING, data, 6);  // 5+1
	// .delay	2000;		//等待
}



/**
 * @brief xc rf adv init
 *
 * @param[in]  void
 * @param[out] void
 */
void rf_adv_init(void)
{

    // /* 打开PWR_ON、EN_PM，解复位 */
    // rf_spi_write_Reg(W_REG | CFG_TOP, 0x02);
	RFAddr = W_REG | CFG_TOP;
	RFWData = 0x02;
	RF_WriteReg();
	// Delay_Ms(2);
	.delay	2000;		//等待

	// rf_spi_write_Reg(W_REG | CFG_TOP, 0x3e);
	RFAddr = W_REG | CFG_TOP;
	RFWData = 0x3e;
	RF_WriteReg();
	// Delay_Ms(2);
	.delay	2000;		//等待


	// rf_spi_read_buff(ANALOG_CFG2, rf_param, 16);
	RFAddr = ANALOG_CFG2;
	DataLen = 16;
	RF_ReadBuf();

	// setbit_array(rf_param, 124);
	// setbit_array(rf_param, 125);
	// ((x)[(y)>>3] |=  (1U << ((y)&7)));
	// ((x)[(y)>>3] |=  (1U << ((y)&7)));

	RXbufData[15] |= (1 << 4) | (1 << 5);
	TXbufData[0] = RXbufData[0];
	TXbufData[1] = RXbufData[1];
	TXbufData[2] = RXbufData[2];
	TXbufData[3] = RXbufData[3];
	TXbufData[4] = RXbufData[4];
	TXbufData[5] = RXbufData[5];
	TXbufData[6] = RXbufData[6];
	TXbufData[7] = RXbufData[7];
	TXbufData[8] = RXbufData[8];
	TXbufData[9] = RXbufData[9];
	TXbufData[10] = RXbufData[10];
	TXbufData[11] = RXbufData[11];
	TXbufData[12] = RXbufData[12];
	TXbufData[13] = RXbufData[13];
	TXbufData[14] = RXbufData[14];
	TXbufData[15] = RXbufData[15];

    // rf_spi_write_buff(W_REG | ANALOG_CFG2, rf_param, 16);
	RFAddr = W_REG | ANALOG_CFG2;
	DataLen = 16;
	RF_WriteBuf();


	// rf_spi_read_buff(PGA_SETTING, rf_param, 5);
	RFAddr = PGA_SETTING;
	DataLen = 5;
	RF_ReadBuf();

	TXbufData[0]=0x44;
	TXbufData[1]=0x3E;
	TXbufData[2]=0x38;
	TXbufData[3]=0x32;
	TXbufData[4]=0x2A;

    // rf_spi_write_buff(W_REG | PGA_SETTING, rf_param, 5);
	RFAddr = W_REG | PGA_SETTING;
	DataLen = 5;
	RF_WriteBuf();


    /* 设置接收、发送地址（4byte）*/
    // rf_spi_write_buff(W_REG | TX_ADDR, tx_adv_Addr, 4);
	RFAddr = W_REG | TX_ADDR;
	TXbufData[0] = 0x6b;
	TXbufData[1] = 0x7d;
	TXbufData[2] = 0x91;
	TXbufData[3] = 0x71;
	DataLen = 4;
	RF_WriteBuf();

    // rf_spi_write_buff(W_REG | RX_ADDR_P0, rx_adv_Addr, 4);
	RFAddr = W_REG | RX_ADDR_P0;
	TXbufData[0] = 0x6b;
	TXbufData[1] = 0x7d;
	TXbufData[2] = 0x91;
	TXbufData[3] = 0x71;
	DataLen = 4;
	RF_WriteBuf();

    // rf_spi_write_Reg(W_REG | SETUP_AW, 0xaa);
	//------------------------------------
	RFAddr = W_REG | SETUP_AW;
	RFWData = 0xaa; //PLL Locking=95us,PTX=4byte,PRX=4byte
	RF_WriteReg();

    /* 设置管道数据长度 */
	TXbufData[0] = BUFF_LEN;
    // rf_spi_write_buff(W_REG | RX_PW_PX, rf_param, 1);
	RFAddr = W_REG | RX_PW_PX;
	DataLen = 1;
	RF_WriteBuf();


    /* 设置包格式 */
    // rf_spi_write_Reg(W_REG | FEATURE, 0x20);
	RFAddr = W_REG | FEATURE;
	RFWData = 0x20;
	RF_WriteReg();

    /* 设置速率、功率 */
    TXbufData[0] = DR_1M;
    TXbufData[1] = RF_PWR;
    // rf_spi_write_buff(W_REG | SETUP_RF, rf_param, 2);
	RFAddr = W_REG | SETUP_RF;
	DataLen = 2;
	RF_WriteBuf();

    // rf_spi_write_Reg(W_REG | SETUP_RETR, 0x30);
	RFAddr = W_REG | SETUP_RETR;
	RFWData = 0x30;
	RF_WriteReg();

    /* 设置模式 */
	if(Trans_Mode == TX_MODE)
	{
		// rf_spi_write_Reg(W_REG | TXPROC_CFG, 0xee);
		RFAddr = W_REG | TXPROC_CFG;
		RFWData = 0xee;
		RF_WriteReg();
		TXbufData[0] = 0x8e;
		TXbufData[1] = 0x82;
	}
	else
	{
		TXbufData[0] = 0x8e;
		TXbufData[1] = 0xc2;
	}

    // rf_spi_write_buff(W_REG | CFG_TOP, rf_param, 2);
	RFAddr = W_REG | CFG_TOP;
	DataLen = 2;
	RF_WriteBuf();


    /* 清除所有中断 */
    // XC_WRITE_REG(W_REG | STATUS, (XC_READ_REG(STATUS) | RX_DR | TX_DS | MAX_RT));
	// byte ReadTemp = rf_spi_read_reg(STATUS);
	RFAddr = STATUS;
	RF_ReadReg();
	
    // rf_spi_write_Reg(W_REG | STATUS, (ReadTemp | RX_DR | TX_DS | MAX_RT));
	RFAddr = W_REG | STATUS;
	RFWData = (RFRData | RX_DR | TX_DS | MAX_RT);
	RF_WriteReg();

    /* 清除TX FIFO、RX FIFO */
    // rf_spi_write_Reg(FLUSH_TX, CMD_NOP);
	RFAddr = FLUSH_TX;
	RFWData = CMD_NOP;
	RF_WriteReg();

    // rf_spi_write_Reg(FLUSH_RX, CMD_NOP);
	RFAddr = FLUSH_RX;
	RFWData = CMD_NOP;
	RF_WriteReg();

}


// void rf_adv_data_config(uint8_t data)
// {

//     tx_buff[0] = 0x42;                		// 广播类型：非连接广播
//     tx_buff[1] = PIPE0_ADV_DATA_LEN - 5; 	// 有效数据长度
	
//     // 固定 MAC 地址
//     // uint8_t mac_buff[6] = {0xa6, 0xa5, 0xa4, 0xa3, 0xa2, 0xc1};
// 	tx_buff[2] = 0xa6;
// 	tx_buff[3] = 0xa5;
// 	tx_buff[4] = 0xa4;
// 	tx_buff[5] = 0xa3;
// 	tx_buff[6] = 0xa2;
// 	tx_buff[7] = 0xc1;


//     tx_buff[8] = 0x02;    // 广播标志段长度
//     tx_buff[9] = 0x01;    // 类型：标志
//     tx_buff[10] = 0x06;   // 标志内容
//     // tx_buff[11] = PIPE0_ADV_DATA_LEN - 15;  // 名称段长度
//     tx_buff[11] = 0x0A;  // 名称段长度

//     tx_buff[12] = 0x09;                     // 类型：设备名称

// 	tx_buff[13] = 0x4C; // L
// 	tx_buff[14] = 0x58; // X
// 	tx_buff[15] = 0x5F; // _
// 	tx_buff[16] = 0x44; // D
// 	tx_buff[17] = 0x58; // X
// 	tx_buff[18] = 0x30; // 0
// 	tx_buff[19] = 0x30; // 0
// 	tx_buff[20] = 0x31; // 1

// 	tx_buff[21] = data; // 
// 	tx_buff[22] = 0x0; // 
// 	tx_buff[23] = 0x0; // 
// 	tx_buff[24] = 0x0; // 
// 	tx_buff[25] = 0x0; // 
// 	tx_buff[26] = 0x0; // 
// 	tx_buff[27] = 0x0; // 
// 	tx_buff[28] = 0x0; // 
// 	tx_buff[29] = 0x0; // 
// 	tx_buff[30] = 0x0; // 
// 	tx_buff[31] = 0x0; // 
// 	tx_buff[32] = 0x0; // 
// 	tx_buff[33] = 0x0; // 
// 	tx_buff[34] = 0x0; // 
// 	tx_buff[35] = 0x0; // 
// 	tx_buff[36] = 0x0; // 
// 	tx_buff[37] = 0x0; // 
// 	tx_buff[38] = 0x0; // 	


// 	//设备名称
// 	// tx_buff[13] = 0x44; // D
// 	// tx_buff[14] = 0x50; // P
// 	// tx_buff[15] = 0x38; // 8
// 	// tx_buff[16] = 0x32; // 2
// 	// tx_buff[17] = 0x30; // 0
// 	// tx_buff[18] = 0x2D; // -
// 	// tx_buff[19] = 0x30; // 0
// 	// tx_buff[20] = 0x31; // 1
// 	// tx_buff[21] = 0x32; // 2
// 	// tx_buff[22] = 0x33; // 3
// 	// tx_buff[23] = 0x34; // 4
// 	// tx_buff[24] = 0x35; // 5
// 	// tx_buff[25] = 0x36; // 6
// 	// tx_buff[26] = 0x37; // 7
// 	// tx_buff[27] = 0x38; // 8
// 	// tx_buff[28] = 0x39; // 9
// 	// tx_buff[29] = 0x2D; // -
// 	// tx_buff[30] = 0x41; // A
// 	// tx_buff[31] = 0x42; // B
// 	// tx_buff[32] = 0x43; // C
// 	// tx_buff[33] = 0x44; // D
// 	// tx_buff[34] = 0x45; // E
// 	// tx_buff[35] = 0x46; // F
// 	// tx_buff[36] = 0x47; // G
// 	// tx_buff[37] = 0x48; // H
// 	// tx_buff[38] = 0x49; // I


// 	// 最后 3 字节填 0x55 → 留给 CRC 计算
//     tx_buff[PIPE0_ADV_DATA_LEN - 3] = 0x55;
//     tx_buff[PIPE0_ADV_DATA_LEN - 2] = 0x55;
//     tx_buff[PIPE0_ADV_DATA_LEN - 1] = 0x55;


// }


// byte Get_Device_CheckNum(void)
// {
// 	uint8_t i = 2;
// 	uint32_t sum = 0;

// 	while (i < 8)
// 	{
// 		sum += TXbufData[i];
// 		i++;
// 		/* code */
// 	}
	

// 	return (sum & 0xFF);

// }

void rf_adv_data_config(void)
{

	TXbufData[0] = 0x42;                	// 广播类型：非连接广播
    TXbufData[1] = BUFF_LEN - 5; 	// 有效数据长度
	
    // 固定 MAC 地址
    // uint8_t mac_buff[6] = {0xa6, 0xa5, 0xa4, 0xa3, 0xa2, 0xc1};
	TXbufData[2] = 0x01;
	TXbufData[3] = 0x01;
	TXbufData[4] = 0x01;
	TXbufData[5] = 0x00;
	TXbufData[6] = 0x00;
	TXbufData[7] = 0x01;


    TXbufData[8] = 0x02;    // 广播标志段长度
    TXbufData[9] = 0x01;    // 类型：标志
    TXbufData[10] = 0x06;   // 标志内容
    // TXbufData[11] = BUFF_LEN - 15;  // 名称段长度
    TXbufData[11] = 0x0B;  // 名称段长度
    TXbufData[12] = 0x09;                     // 类型：设备名称

	TXbufData[13] = 0x4C; // L
	TXbufData[14] = 0x58; // X
	TXbufData[15] = 0x01; // 方案商代号
	TXbufData[16] = 0x01; // 设备代号
	TXbufData[17] = 0x01; // 客户代码
	TXbufData[18] = 0x00; // 0
	TXbufData[19] = 0x00; // 0
	TXbufData[20] = 0x01; // 1

	TXbufData[21] = 0; // 

	// 设备端广播校验码
	TXbufData[22] = 0x55; // 
	TXbufData[23] = 0x0; // 
	TXbufData[24] = 0x0; // 
	TXbufData[25] = 0x0; // 
	TXbufData[26] = 0x0; // 
	TXbufData[27] = 0x0; // 
	TXbufData[28] = 0x0; // 
	TXbufData[29] = 0x0; // 
	TXbufData[30] = 0x0; // 
	TXbufData[31] = 0x0; // 
	TXbufData[32] = 0x0; // 
	TXbufData[33] = 0x0; // 
	TXbufData[34] = 0x0; // 
	TXbufData[35] = 0x0; // 
	TXbufData[36] = 0x0; // 
	TXbufData[37] = 0x0; // 
	TXbufData[38] = 0x0; // 	


	//设备名称
	// TXbufData[13] = 0x44; // D
	// TXbufData[14] = 0x50; // P
	// TXbufData[15] = 0x38; // 8
	// TXbufData[16] = 0x32; // 2
	// TXbufData[17] = 0x30; // 0
	// TXbufData[18] = 0x2D; // -
	// TXbufData[19] = 0x30; // 0
	// TXbufData[20] = 0x31; // 1
	// TXbufData[21] = 0x32; // 2
	// TXbufData[22] = 0x33; // 3
	// TXbufData[23] = 0x34; // 4
	// TXbufData[24] = 0x35; // 5
	// TXbufData[25] = 0x36; // 6
	// TXbufData[26] = 0x37; // 7
	// TXbufData[27] = 0x38; // 8
	// TXbufData[28] = 0x39; // 9
	// TXbufData[29] = 0x2D; // -
	// TXbufData[30] = 0x41; // A
	// TXbufData[31] = 0x42; // B
	// TXbufData[32] = 0x43; // C
	// TXbufData[33] = 0x44; // D
	// TXbufData[34] = 0x45; // E
	// TXbufData[35] = 0x46; // F
	// TXbufData[36] = 0x47; // G
	// TXbufData[37] = 0x48; // H
	// TXbufData[38] = 0x49; // I


	// 最后 3 字节填 0x55 → 留给 CRC 计算
    TXbufData[BUFF_LEN - 3] = 0x55;
    TXbufData[BUFF_LEN - 2] = 0x55;
    TXbufData[BUFF_LEN - 1] = 0x55;


}


// void rf_setChannel(uint16_t channel)
// {
//     uint8_t ch_val[2];

//     ch_val[0] = channel & 0xFF;        // 低 8 位
//     ch_val[1] = channel >> 8;   // 高 8 位

//     rf_spi_write_Reg(W_REG | EN_AA, 0x00);    // 先关闭自动应答
//     rf_spi_write_buff(W_REG | RF_CH, ch_val, 2); // 写入 16 位信道
//     rf_spi_write_Reg(W_REG | EN_AA, ENAA_SET_VAL); // 恢复自动应答

// }

void rf_setchannel_tx(void)
{

    TXbufData[0] = rf_adv_channel[adv_idx] & 0xFF;        // 低 8 位
    TXbufData[1] = rf_adv_channel[adv_idx] >> 8;   // 高 8 位

    // rf_spi_write_Reg(W_REG | EN_AA, 0x00);    // 先关闭自动应答
	RFAddr = W_REG | EN_AA;
	RFWData = 0x00;
	RF_WriteReg();

    // rf_spi_write_buff(W_REG | RF_CH, ch_val, 2); // 写入 16 位信道
	RFAddr = W_REG | RF_CH;
	DataLen = 2;
	RF_WriteBuf();

    // rf_spi_write_Reg(W_REG | EN_AA, ENAA_SET_VAL); // 恢复自动应答
	RFAddr = W_REG | EN_AA;
	RFWData = ENAA_SET_VAL;
	RF_WriteReg();

}

void rf_setchannel_rx(void)
{

    TXbufData[0] = (rf_adv_channel[adv_idx]-1) & 0xFF;        // 低 8 位
    TXbufData[1] = (rf_adv_channel[adv_idx]-1) >> 8;   // 高 8 位

    // rf_spi_write_Reg(W_REG | EN_AA, 0x00);    // 先关闭自动应答
	RFAddr = W_REG | EN_AA;		//21
	RFWData = 0x00;
	RF_WriteReg();

    // rf_spi_write_buff(W_REG | RF_CH, ch_val, 2); // 写入 16 位信道
	RFAddr = W_REG | RF_CH;		//25 61 09
	DataLen = 2;
	RF_WriteBuf();

    // rf_spi_write_Reg(W_REG | EN_AA, ENAA_SET_VAL); // 恢复自动应答
	RFAddr = W_REG | EN_AA;		//21
	RFWData = ENAA_SET_VAL;		//41
	RF_WriteReg();

}


/**
 * @brief rf reversebits
 *
 * @param input
 * @return uint8_t
 */
uint8_t rf_reversebits(uint8_t input)
{
    uint8_t temp = 0;
    uint8_t i = 0;

    do
    {
        temp <<= 1;               // temp 左移腾出最低位
        if(input & 0x01)          // 取 input 最低位
        {
            temp |= 0x01;         // 如果是1，写入temp最低位
        }
        input >>= 1;              // input 右移，准备取下一位
        i++;
    } while (i < 8);

    return temp;
}


// void rf_ble_crc_tx(uint8_t buff_len)
// {
//     uint8_t temp;
//     uint8_t temp_data;
//     uint8_t i;
//     uint8_t idx = 0;

//     // CRC 存放位置（最后3字节）
//     uint8_t crc0;
//     uint8_t crc1;
//     uint8_t crc2;

//     // 先把数组里的值读出来（不直接在运算里操作数组）
//     crc0 = tx_buff[buff_len];
//     crc1 = tx_buff[buff_len + 1];
//     crc2 = tx_buff[buff_len + 2];

//     // 遍历所有数据字节
//     while (idx < buff_len)
//     {
//         temp_data = tx_buff[idx];
//         idx++;

//         i = 0;
//         // 8位 循环
//         while (i < 8)
//         {
//             // 第一步：取最高位
//             temp = crc0 >> 7;

//             // 第二步：crc0 左移
//             crc0 = crc0 << 1;

//             // 第三步：判断crc1最高位，给crc0补位（分步判断，不混合运算）
//             uint8_t bit1 = crc1 & 0x80;
//             if(bit1 != 0)
//             {
//                 crc0 = crc0 | 0x01;
//             }

//             // 第四步：crc1 左移
//             crc1 = crc1 << 1;

//             // 第五步：判断crc2最高位，给crc1补位
//             uint8_t bit2 = crc2 & 0x80;
//             if(bit2 != 0)
//             {
//                 crc1 = crc1 | 0x01;
//             }

//             // 第六步：crc2 左移
//             crc2 = crc2 << 1;

//             // 第七步：简单判断，不混合数组操作
//             uint8_t data_bit = temp_data & 0x01;
//             if(temp != data_bit)
//             {
//                 crc2 = crc2 ^ 0x5B;
//                 crc1 = crc1 ^ 0x06;
//             }

//             // 数据移位
//             temp_data = temp_data >> 1;
//             i++;
//         }
//     }

//     // 位反转
//     crc2 = rf_reversebits(crc2);
//     crc1 = rf_reversebits(crc1);
//     crc0 = rf_reversebits(crc0);

//     // 最后写回数组
//     tx_buff[buff_len]     = crc0;
//     tx_buff[buff_len + 1] = crc1;
//     tx_buff[buff_len + 2] = crc2;
// }

// void rf_ble_crc_tx(uint8_t buff_len)
// {
//     uint8_t temp;
//     uint8_t temp_data;
//     uint8_t i;
//     uint8_t idx = 0;

//     // CRC 存放位置（最后3字节）
//     uint8_t crc0;
//     uint8_t crc1;
//     uint8_t crc2;

//     // 先把数组里的值读出来（不直接在运算里操作数组）
//     crc0 = tx_buff[buff_len];
//     crc1 = tx_buff[buff_len + 1];
//     crc2 = tx_buff[buff_len + 2];

//     // 遍历所有数据字节
//     while (idx < buff_len)
//     {
//         temp_data = tx_buff[idx];
//         idx++;

//         i = 0;
//         // 8位 循环
//         while (i < 8)
//         {
//             // 第一步：取最高位
//             temp = crc0 >> 7;

//             // 第二步：crc0 左移
//             crc0 = crc0 << 1;

//             // 第三步：判断crc1最高位，给crc0补位（分步判断，不混合运算）
//             uint8_t bit1 = crc1 & 0x80;
//             if(bit1 != 0)
//             {
//                 crc0 = crc0 | 0x01;
//             }

//             // 第四步：crc1 左移
//             crc1 = crc1 << 1;

//             // 第五步：判断crc2最高位，给crc1补位
//             uint8_t bit2 = crc2 & 0x80;
//             if(bit2 != 0)
//             {
//                 crc1 = crc1 | 0x01;
//             }

//             // 第六步：crc2 左移
//             crc2 = crc2 << 1;

//             // 第七步：简单判断，不混合数组操作
//             uint8_t data_bit = temp_data & 0x01;
//             if(temp != data_bit)
//             {
//                 crc2 = crc2 ^ 0x5B;
//                 crc1 = crc1 ^ 0x06;
//             }

//             // 数据移位
//             temp_data = temp_data >> 1;
//             i++;
//         }
//     }

//     // 位反转
//     crc2 = rf_reversebits(crc2);
//     crc1 = rf_reversebits(crc1);
//     crc0 = rf_reversebits(crc0);

//     // 最后写回数组
//     tx_buff[buff_len]     = crc0;
//     tx_buff[buff_len + 1] = crc1;
//     tx_buff[buff_len + 2] = crc2;
// }


void rf_ble_crc_tx(void)
{
    uint8_t temp;
    uint8_t temp_data;
    uint8_t i;
    uint8_t idx = 0;

    // CRC 存放位置（最后3字节）
    uint8_t crc0;
    uint8_t crc1;
    uint8_t crc2;

	uint8_t Data_len = BUFF_LEN-3;

    // 先把数组里的值读出来（不直接在运算里操作数组）
    crc0 = TXbufData[Data_len];
    crc1 = TXbufData[Data_len + 1];
    crc2 = TXbufData[Data_len + 2];

    // 遍历所有数据字节
    while (idx < Data_len)
    {
        temp_data = TXbufData[idx];
        idx++;

        i = 0;
        // 8位 循环
        while (i < 8)
        {
            // 第一步：取最高位
            temp = crc0 >> 7;

            // 第二步：crc0 左移
            crc0 = crc0 << 1;

            // 第三步：判断crc1最高位，给crc0补位（分步判断，不混合运算）
            uint8_t bit1 = crc1 & 0x80;
            if(bit1 != 0)
            {
                crc0 = crc0 | 0x01;
            }

            // 第四步：crc1 左移
            crc1 = crc1 << 1;

            // 第五步：判断crc2最高位，给crc1补位
            uint8_t bit2 = crc2 & 0x80;
            if(bit2 != 0)
            {
                crc1 = crc1 | 0x01;
            }

            // 第六步：crc2 左移
            crc2 = crc2 << 1;

            // 第七步：简单判断，不混合数组操作
            uint8_t data_bit = temp_data & 0x01;
            if(temp != data_bit)
            {
                crc2 = crc2 ^ 0x5B;
                crc1 = crc1 ^ 0x06;
            }

            // 数据移位
            temp_data = temp_data >> 1;
            i++;
        }
    }

    // 位反转
    crc2 = rf_reversebits(crc2);
    crc1 = rf_reversebits(crc1);
    crc0 = rf_reversebits(crc0);

    // 最后写回数组
    TXbufData[Data_len]     = crc0;
    TXbufData[Data_len + 1] = crc1;
    TXbufData[Data_len + 2] = crc2;


}


void rf_ble_crc_rx(uint8_t data_len)
{
    uint8_t temp;
    uint8_t temp_data;
    uint8_t i;
    uint8_t idx = 0;

    // CRC 存放位置（最后3字节）
    uint8_t crc0;
    uint8_t crc1;
    uint8_t crc2;

    // 先把数组里的值读出来（不直接在运算里操作数组）
    crc0 = RXbufData[data_len];
    crc1 = RXbufData[data_len + 1];
    crc2 = RXbufData[data_len + 2];

    // 遍历所有数据字节
    while (idx < data_len)
    {
        temp_data = RXbufData[idx];
        idx++;

        i = 0;
        // 8位 循环
        while (i < 8)
        {
            // 第一步：取最高位
            temp = crc0 >> 7;

            // 第二步：crc0 左移
            crc0 = crc0 << 1;

            // 第三步：判断crc1最高位，给crc0补位（分步判断，不混合运算）
            uint8_t bit1 = crc1 & 0x80;
            if(bit1 != 0)
            {
                crc0 = crc0 | 0x01;
            }

            // 第四步：crc1 左移
            crc1 = crc1 << 1;

            // 第五步：判断crc2最高位，给crc1补位
            uint8_t bit2 = crc2 & 0x80;
            if(bit2 != 0)
            {
                crc1 = crc1 | 0x01;
            }

            // 第六步：crc2 左移
            crc2 = crc2 << 1;

            // 第七步：简单判断，不混合数组操作
            uint8_t data_bit = temp_data & 0x01;
            if(temp != data_bit)
            {
                crc2 = crc2 ^ 0x5B;
                crc1 = crc1 ^ 0x06;
            }

            // 数据移位
            temp_data = temp_data >> 1;
            i++;
        }
    }

    // 位反转
    crc2 = rf_reversebits(crc2);
    crc1 = rf_reversebits(crc1);
    crc0 = rf_reversebits(crc0);

    // 最后写回数组
    RXbufData[data_len]     = crc0;
    RXbufData[data_len + 1] = crc1;
    RXbufData[data_len + 2] = crc2;

}



uint8_t rf_ble_whiten_start(void)
{
    uint8_t channel;
    uint8_t fun_data;
	uint16_t fre = rf_adv_channel[adv_idx];

    // 判断信道编号
    if(fre == RF_ADV_CHANNEL_37)
    {
        channel = 37;
    }
    else if(fre == RF_ADV_CHANNEL_38)
    {
        channel = 38;
    }
    else
    {
        channel = 39;
    }

	fun_data = rf_reversebits(channel);
    // 位反转后 | 2，返回
    return (fun_data | 0x02);
}


//TX数据进行白化处理
void rf_ble_whiten_tx(void)
{
    uint8_t whiltrncoeff = rf_ble_whiten_start();
    uint8_t data_len = BUFF_LEN;
    uint16_t idx = 0;
    uint8_t i;
    uint8_t fun_data;

    while (data_len--)
    {
        i = 1;
        while (i)
        {
            if (whiltrncoeff & 0x80)
            {
                whiltrncoeff ^= 0x11;
                TXbufData[idx] ^= i;
            }
            whiltrncoeff <<= 1;
            i <<= 1;
        }

    	fun_data = rf_reversebits(TXbufData[idx]);

        TXbufData[idx] = fun_data;

        idx++;

    }

}


//RX数据进行白化处理
// void rf_ble_whiten_rx(byte len)
// {
//     uint8_t whiltrncoeff = rf_ble_whiten_start();
//     uint8_t buff_len = bu;
//     uint16_t idx = 0;
//     uint8_t i;
//     uint8_t fun_data;

//     while (buff_len--)
//     {
//         i = 1;
//         while (i)
//         {
//             if (whiltrncoeff & 0x80)
//             {
//                 whiltrncoeff ^= 0x11;
//                 rx_buff[idx] ^= i;
//             }
//             whiltrncoeff <<= 1;
//             i <<= 1;
//         }

//     	fun_data = rf_reversebits(rx_buff[idx]);

//         rx_buff[idx] = fun_data;

//         idx++;

//     }

// }


//RX数据进行白化处理
void rf_ble_whiten_rx(void)
{
    uint8_t whiltrncoeff = rf_ble_whiten_start();
    uint8_t data_len = BUFF_LEN;
    uint16_t idx = 0;
    uint8_t i;
    uint8_t fun_data;

    while (data_len--)
    {
        i = 1;
        while (i)
        {
            if (whiltrncoeff & 0x80)
            {
                whiltrncoeff ^= 0x11;
                RXbufData[idx] ^= i;
            }
            whiltrncoeff <<= 1;
            i <<= 1;
        }

    	fun_data = rf_reversebits(RXbufData[idx]);

        RXbufData[idx] = fun_data;

        idx++;

    }

}

// uint8_t rf_tx_packet(uint8_t *buff, uint8_t len)
// {
//     uint8_t rett = 1;

//     uint8_t cfg_val;
//     uint8_t fifo_stat;

//     // 读取FIFO状态 → 判断TX FIFO是否未满（新写法）
//     fifo_stat = rf_spi_read_reg(STATUS_FIFO);
//     // 判断TX FIFO是否未满
//     if (!(fifo_stat & STAT_TX_FULL))
//     {
//         // 写入发送数据
//         rf_spi_write_buff(W_TX_PLOAD, buff, len);

//         // // ==================== CE_HIGH 展开实现 ====================
//         cfg_val = rf_spi_read_reg(CFG_TOP);       // 读取CFG_TOP寄存器
//         cfg_val |= 0x01;                      // 第0位置1
//         rf_spi_write_Reg(W_REG | CFG_TOP, cfg_val);

//         // Delay_Us(50);                         // 延时50us
// 		.delay	50;		//等待

//         // ==================== CE_LOW  展开实现 ====================
//         cfg_val = rf_spi_read_reg(CFG_TOP);       // 重新读取CFG_TOP寄存器
//         cfg_val &= 0xFE;                      // 第0位清0
//         rf_spi_write_Reg(W_REG | CFG_TOP, cfg_val);

//         rett = 0;  // 发送成功
//     }
//     else
//     {
//         // TX FIFO满，清空FIFO
//         rf_spi_write_Reg(FLUSH_TX, CMD_NOP);
// 		Log_Buf_Out[0] = 0xEE;	// 错误代码示例
// 		Log_Buf_Out[1] = 0xEE;	// 错误代码示例

// 		UART_HandShake(0xFF,Log_Buf_Out , 2);  // 发送错误信息
//         rett = 1;
//     }

//     return rett;
// }

void CE_High(void)
{
	RFAddr=CFG_TOP;   //00
	RF_ReadReg();		//8E
	RFAddr = W_REG | CFG_TOP;	//20
	RFWData = RFRData | 0x01;   //8F
	RF_WriteReg();
}
//-------------------------------
void CE_Low(void)
{
	RFAddr=CFG_TOP;
	RF_ReadReg();
	RFAddr = W_REG | CFG_TOP;
	RFWData = RFRData & 0xFE;
	RF_WriteReg();
}


void rf_tx_packet(void)
{
    uint8_t data_len;
    uint8_t cfg_val;
    uint8_t fifo_stat;

	data_len = BUFF_LEN;
    // 读取FIFO状态 → 判断TX FIFO是否未满（新写法）
    // fifo_stat = rf_spi_read_reg(STATUS_FIFO);
	RFAddr = STATUS_FIFO;
	RF_ReadReg();
	fifo_stat = RFRData;
    // 判断TX FIFO是否未满
    if (!(fifo_stat & STAT_TX_FULL))
    {
        // 写入发送数据
        // rf_spi_write_buff(W_TX_PLOAD, buff, len);
		RFAddr = W_TX_PLOAD;
		DataLen = data_len;
		RF_WriteBuf();

        // // ==================== CE_HIGH 展开实现 ====================
        // cfg_val = rf_spi_read_reg(CFG_TOP);       // 读取CFG_TOP寄存器
        // cfg_val |= 0x01;                      // 第0位置1
        // rf_spi_write_Reg(W_REG | CFG_TOP, cfg_val);
		CE_High();

        // Delay_Us(50);                         // 延时50us
		.delay	50;		//等待

        // ==================== CE_LOW  展开实现 ====================
        // cfg_val = rf_spi_read_reg(CFG_TOP);       // 重新读取CFG_TOP寄存器
        // cfg_val &= 0xFE;                      // 第0位清0
        // rf_spi_write_Reg(W_REG | CFG_TOP, cfg_val);
		CE_Low();

    }
    else
    {
        // TX FIFO满，清空FIFO
        // rf_spi_write_Reg(FLUSH_TX, CMD_NOP);
		RFAddr = FLUSH_TX;
		RFWData = CMD_NOP;
		RF_WriteReg();
#if DEBUG_EN
		UART_Send_Byte(0xee);
#endif
		// Log_Buf_Out[0] = 0xEE;	// 错误代码示例
		// Log_Buf_Out[1] = 0xEE;	// 错误代码示例
		// UART_HandShake(0xFF,Log_Buf_Out , 2);  // 发送错误信息
    }

}




/**
 * @brief       rf receiver function
 * @param       void
 * @retval      void
 */
// byte rf_rx_packet(uint8_t *buff)
// {
//     uint8_t stat,stat1, len,cfg_val = 0;

//     stat  = rf_spi_read_reg(STATUS);

// 	stat1 = stat & RX_DR;

//     if (stat1 == RX_DR)
// 	{

		
//         // CE_CTL_LOW;
//         // ==================== CE_LOW  展开实现 ====================
//         cfg_val = rf_spi_read_reg(CFG_TOP);       // 重新读取CFG_TOP寄存器
//         cfg_val &= 0xFE;                      // 第0位清0
//         rf_spi_write_Reg(W_REG | CFG_TOP, cfg_val);

//         len = rf_spi_read_reg(R_RX_PL_WID);

// 		// UART_Send_Byte(RX_len);

// 		// rf_spi_read_buff_rx(R_RX_PLOAD, len);
// 		rf_spi_read_buff(R_RX_PLOAD,buff,len);

// 		rf_spi_write_Reg(W_REG | STATUS, stat1);
// 		rf_spi_write_Reg(FLUSH_RX, CMD_NOP);

//     }

//     return len;

// }


/**
 * @brief       rf receiver function
 * @param       void
 * @retval      void
 */
byte rf_rx_packet(void)
{
    uint8_t stat,stat1, len = 0,rx_l = 0;

    // stat  = rf_spi_read_reg(STATUS);
	RFAddr = STATUS;  //07
	RF_ReadReg();
	stat = RFRData;

	// UART_Send_Byte(stat);	

	stat1 = RFRData & RX_DR;	//RX_DR==0x40

	// UART_Send_Byte(stat1);	

    if (stat1 == RX_DR)
	{
        // CE_CTL_LOW;
        // ==================== CE_LOW  展开实现 ====================
        // cfg_val = rf_spi_read_reg(CFG_TOP);       // 重新读取CFG_TOP寄存器
        // cfg_val &= 0xFE;                      // 第0位清0
        // rf_spi_write_Reg(W_REG | CFG_TOP, cfg_val);
		CE_Low();		//00 8e--> 20 8f

        // len = rf_spi_read_reg(R_RX_PL_WID);
		RFAddr = R_RX_PL_WID;		//0x60
		RF_ReadReg();
		len = RFRData;

		// UART_Send_Byte(RX_len);

		if((len == 0) || (len > BUFF_LEN))
		{
			RFAddr = W_REG | STATUS;
			RFWData = stat1;
			RF_WriteReg();

			RFAddr = FLUSH_RX;
			RFWData = CMD_NOP;
			RF_WriteReg();
			CE_High();
			return 0;
		}

		//考虑RXbufData[15]清零
		// rf_spi_read_buff_rx(R_RX_PLOAD, len);
		// rf_spi_read_buff(R_RX_PLOAD,buff,len);
		while(rx_l < BUFF_LEN)  // 
		{
			RXbufData[rx_l] = 0x00;
			rx_l++;
		}
			
		RFAddr = R_RX_PLOAD;	//0x61
		DataLen = len;
		RF_ReadBuf();

		// rf_spi_write_Reg(W_REG | STATUS, stat);
		RFAddr = W_REG | STATUS;   //27
		RFWData = stat1;				//40
		RF_WriteReg();

		// rf_spi_write_Reg(FLUSH_RX, CMD_NOP);
		RFAddr = FLUSH_RX; 			//E2
		RFWData = CMD_NOP;			//FF
		RF_WriteReg();

    }

    return len;

}




/**

//  */
void rf_rx_init(void)
{
    adv_idx = 0;

    //rf_setChannel(rf_adv_channel[adv_idx] - 1);
 	rf_setchannel_rx();

    // CE_CTL_HIGH;	
    // cfg_val = rf_spi_read_reg(CFG_TOP);       // 读取CFG_TOP寄存器
    // cfg_val |= 0x01;                      // 第0位置1
    // rf_spi_write_Reg(W_REG | CFG_TOP, cfg_val);
	CE_High();  //008e	208f

	.delay	1000;		//等待

}




/**
 * @brief       XC81x RF sleep or reset
 * @param       void
 * @retval      void
 */
void rf_sleep_reset(void)
{
    uint8_t cfg_val;

	// CE_CTL_LOW;
	// ==================== CE_LOW  展开实现 ====================
	cfg_val = rf_spi_read_reg(CFG_TOP);       // 重新读取CFG_TOP寄存器
	cfg_val &= 0xFE;                      // 第0位清0
	rf_spi_write_Reg(W_REG | CFG_TOP, cfg_val);

    /* Sleep and reset (sleep current 2.1ua,performing sleep reset rf) */
    rf_spi_write_Reg(W_REG | CFG_TOP, 0x80);
}


void rf_wakeup(void)
{
	uint8_t cfg_val;

    // // CE_CTL_LOW;
    // // ==================== CE_LOW  展开实现 ====================
    // cfg_val = rf_spi_read_reg(CFG_TOP);       // 重新读取CFG_TOP寄存器
    // cfg_val &= 0xFE;                      // 第0位清0
    // rf_spi_write_Reg(W_REG | CFG_TOP, cfg_val);

    rf_spi_write_Reg(W_REG | CFG_TOP, 0x02);
    // Delay_Ms(2);
	.delay	2000;		//等待
    rf_spi_write_Reg(W_REG | CFG_TOP, 0x86);
    rf_spi_write_Reg(W_REG | CFG_TOP, 0x8e);
	.delay	2000;		//等待

	// adv_idx = 0;
    // rf_setChannel(rf_adv_channel[adv_idx] - 1);

	// CE_CTL_HIGH;
	// ==================== CE_HIGH 展开实现 ====================
	cfg_val = rf_spi_read_reg(CFG_TOP);       // 读取CFG_TOP寄存器
	// UART_Send_Data(LOG_Ing,RX_cfg_val);	
	cfg_val |= 0x01;                      // 第0位置1
	// UART_Send_Data(LOG_Ing,RX_cfg_val);	
	rf_spi_write_Reg(W_REG | CFG_TOP, cfg_val);

}

#endif



//==========ADC======================

void	ADC_init(void)//初始化
{
	//注：选择的通道需设置为输入，无上拉、下拉电阻、无数字输入使能；
	PCC.2 = 0;
	PCPH.2 = 0;
	PCPL &= 0xfb;
//	PCPL.2 = 0;			//无下拉的脚位不需要这条指令
	// PCDIER = 0b1111_1011;//注：其他脚默认为1，需要关闭的请手动修改
	//PCDIER = 0b0000_1011;//注：其他脚默认为1，需要关闭的请手动修改
	$ PCDIER 0x00;//请注意其他脚位设置

//	$ PADIER 0b1110_1001;//上面语法错误可用这条替代
	$ ADCC	Enable,PC2;	//启用ADC功能，通道选择（AD转换的输入信号）
						//启用：Enable；停用：Disable
						//通道选择：PB0, PB1, PB2, PB3, PB4, PB5, PB6, PB7,PA3, PA4, PA0
						//注：每个芯片的待测脚位不同，详情参考datasheet
	$ ADCM	12bit,/2;			//时钟源选择（系统时钟/X）
						//X有/1, /2, /4, /8, /16, /32, /64, /128
						//注：时钟源选择建议选用500K（/2）
						//参考高电压为VDD
	.delay 400;			//延时400us
						//注：时钟源选择其他时，延迟时间请参考datasheet
}



void	ADC_mea(void)//测量数据
{
	//开始ADC转换
	AD_START = 1;		//开始ADC转换
	while(!AD_DONE)		//等待ADC转换结果
		NULL;
	//当AD_DONE高电位时读取ADC结果
	//注意：8位分辨率读取ADCR值，9~12为分辨率读取ADCRH和ADCRL
	// data = ADCR;		//将ADC的值赋给data
	ADCdata_12bit$1 = ADCRH;
	ADCdata_12bit$0 = ADCRL;

	ADCdata_12bit = ADCdata_12bit >> 4;

#if 1

	_Temp += ADCdata_12bit;

	ADCnt++;
	if (ADCnt >= 16)
	{

		ADCnt = 0;
		PC2_AD_Data = _Temp >> 4;
		_Temp = 0;
	}

#endif


}


// eword	AD_sum;		//AD测量值得和
// word	AD_ave;		//AD测量值得平均数
// word	AD_max;		//AD测量值中的最大值
// word	AD_min;		//AD测量值中的最小值
// void	Median_ave(void)
// {
// 	ADC_mea();
// 	AD_min = ADCdata_12bit;//测量前先测一个当作最小值
// 	byte num = 10;//测量次数
// 	AD_sum = 0;//清零
// 	while(num--)
// 	{
// 		ADC_mea();
// 		AD_sum += ADCdata_12bit;//将测量的每个数据求和
// 		if(ADCdata_12bit > AD_max)//判断最大值，并记录
// 		{
// 			AD_max = ADCdata_12bit;
// 		}
// 		elseif(ADCdata_12bit >= AD_min)//中间值不记录
// 		{
// 			nop;
// 		}
// 		else//判断最小值，并记录
// 		{
// 			AD_min = ADCdata_12bit;
// 		}
// 	}
// 	//去掉最大值，去掉最小值，取平均得到最终值
// 	AD_sum = AD_sum - AD_min - AD_max;
// 	AD_ave = AD_sum >> 3;

// 	_Temp += AD_ave;

// 	ADCnt++;
// 	if (ADCnt >= 16)
// 	{

// 		ADCnt = 0;
// 		PC2_AD_Data = _Temp >> 8;
// 		_Temp = 0;
// 	}

// }





//==========按键处理======================


// //========PA0(PB5)中断设置========
// //用PB5中断需要在.pre中将PA0改成PB5，用法同PA0
// void	PA0_init(void)
// {
// 	//设置PA0引脚输入模式，根据情况选择是否需要上拉
// 	$ Key_1 in,pull;//
// 	$ INTEGS PA0_F;				//PA0中断边缘选择，上升缘和下降缘上升缘都请求中断PA0_B，上升缘请求中断为PA0_R，下降缘请求中断为PA0_F。
// 	//使用下面两句时，会关闭其他中断的设置，一般推荐使用对位操作的方法来驱动
// //	$ INTEN PA0;				//中断允许寄存器，启用从PA0的溢出中断；1：启用，0：停用。
// //	$ INTRQ PA0;				//中断请求寄存器，此位是由硬件置位并由软件清零；1：请求，0：不请求。

// 	INTEN.PA0 = 1;				//中断允许寄存器，开PA0中断
// //	INTEN.PA0 = 0;				//中断允许寄存器，关PA0中断
// 	INTRQ.PA0 = 0;				//中断请求寄存器，清零INTRQ寄存器。
// }

// //========PB0(PA4)中断设置========
// //用PA4中断需要在.pre中将PB0改成PA4，用法同PB0
// void	PB0_init(void)
// {
// 	//设置PB0引脚输入模式，根据情况选择是否需要上拉
// 	$ PB.0 In;
// 	$ INTEGS PB0_B;				//PB0中断边缘选择，上升缘和下降缘上升缘都请求中断PB0_B，上升缘请求中断为PB0_R，下降缘请求中断为PB0_F。
// 	//使用下面两句时，会关闭其他中断的设置，一般推荐使用对位操作的方法来驱动
// //	$ INTEN PB0;				//中断允许寄存器，启用从PB0的溢出中断；1：启用，0：停用。
// //	$ INTRQ PB0;				//中断请求寄存器，此位是由硬件置位并由软件清零；1：请求，0：不请求。

// 	INTEN.PB0 = 1;				//中断允许寄存器，开PB0中断
// //	INTEN.PB0 = 0;				//中断允许寄存器，关PB0中断
// 	INTRQ.PB0 = 0;				//中断请求寄存器，清零INTRQ寄存器。
// }


//========PB0(PA4)中断设置========
//用PA4中断需要在.pre中将PB0改成PA4，用法同PB0
void	Key_init(void)
{
	//设置PB0引脚输入模式，根据情况选择是否需要上拉
	$ KEY_1_IO in,pull;//
	$ KEY_2_IO in,pull;//
	$ INTEGS PB0_F;				//PB0中断边缘选择，上升缘和下降缘上升缘都请求中断PB0_B，上升缘请求中断为PB0_R，下降缘请求中断为PB0_F。
	//使用下面两句时，会关闭其他中断的设置，一般推荐使用对位操作的方法来驱动
//	$ INTEN PB0;				//中断允许寄存器，启用从PB0的溢出中断；1：启用，0：停用。
//	$ INTRQ PB0;				//中断请求寄存器，此位是由硬件置位并由软件清零；1：请求，0：不请求。

	INTEN.PB0 = 1;				//中断允许寄存器，开PB0中断
//	INTEN.PB0 = 0;				//中断允许寄存器，关PB0中断
	INTRQ.PB0 = 0;				//中断请求寄存器，清零INTRQ寄存器。
}

// //========PA0(PB5)中断设置========
//用PB5中断需要在.pre中将PA0改成PB5，用法同PA0
void	RPIN_init(void)
{
	//设置PA0引脚输入模式，根据情况选择是否需要上拉
	$ RPIN_IO in,pull;//
	$ INTEGS PB5_R;				//PA0中断边缘选择，上升缘和下降缘上升缘都请求中断PA0_B，上升缘请求中断为PA0_R，下降缘请求中断为PA0_F。
	//使用下面两句时，会关闭其他中断的设置，一般推荐使用对位操作的方法来驱动
//	$ INTEN PA0;				//中断允许寄存器，启用从PA0的溢出中断；1：启用，0：停用。
//	$ INTRQ PA0;				//中断请求寄存器，此位是由硬件置位并由软件清零；1：请求，0：不请求。

	INTEN.PB5 = 1;				//中断允许寄存器，开PA0中断
//	INTEN.PA0 = 0;				//中断允许寄存器，关PA0中断
	INTRQ.PB5 = 0;				//中断请求寄存器，清零INTRQ寄存器。
}



/* 按键判断 */
void KeyJudge_1(void)
{
	//按键中断标志
	if(KeyFlag_1)
	{
		RemoveKey1ms_1++;
		if(RemoveKey1ms_1 >= 5)
		{
			if(KEY_1_IO == 0) //有效按下--》判断短按长按
			{
				RemoveKey1ms_1 = 0;
				JugeKeyStateFlag_1 = 1;
			}
			else //松开 无效按下
			{
				RemoveKey1ms_1 = 0;
				KeyFlag_1 = 0;
			}
			
			
		}
	}

	if(JugeKeyStateFlag_1)
	{
		KeyCount1ms_1++;
		if(KeyCount1ms_1 <= 100) 
		{
			if(KEY_1_IO == 1) //松开-->短按
			{
				KeyCount1ms_1 = 0;
				KeyFlag_1 = 0;
				JugeKeyStateFlag_1 = 0;
				ShortKeyFlag_1 = 1;
			}
			
		}
		else  //长按1S
		{
			if(KEY_1_IO == 1) //松开
			{
				KeyCount1ms_1 = 0;
				KeyFlag_1 = 0;
				JugeKeyStateFlag_1 = 0;  //重新开始检测按键
				LongKeyProFlag_1 = 0;
			}
			else if((LongKeyProFlag_1 == 0) && (KEY_1_IO == 0))
			{
				LongKeyProFlag_1 = 1;  // 防重复
				LongKeyFlag_1 = 1;     // 触发长按
			}

			
			
		}
		
	}
	
}	

/* 按键判断 */
void KeyJudge_2(void)
{
	//按键中断标志
	if(KeyFlag_2)
	{
		RemoveKey1ms_2++;
		if(RemoveKey1ms_2 >= 5)
		{
			if(KEY_2_IO == 0) //有效按下--》判断短按长按
			{
				RemoveKey1ms_2 = 0;
				JugeKeyStateFlag_2 = 1;
			}
			else //松开 无效按下
			{
				RemoveKey1ms_2 = 0;
				KeyFlag_2 = 0;
			}
			
			
		}
	}

	if(JugeKeyStateFlag_2)
	{
		KeyCount1ms_2++;
		if(KeyCount1ms_2 <= 100) 
		{
			if(KEY_2_IO == 1) //松开-->短按
			{
				KeyCount1ms_2 = 0;
				KeyFlag_2 = 0;
				JugeKeyStateFlag_2 = 0;
				ShortKeyFlag_2 = 1;
			}
			
		}
		else  //长按1S
		{
			if(KEY_2_IO == 1) //松开
			{
				KeyCount1ms_2 = 0;
				KeyFlag_2 = 0;
				JugeKeyStateFlag_2 = 0;  //重新开始检测按键
				LongKeyProFlag_2 = 0;
			}
			else if((LongKeyProFlag_2 == 0) && (KEY_2_IO == 0))
			{
				LongKeyProFlag_2 = 1;  // 防重复
				LongKeyFlag_2 = 1;     // 触发长按
			}

			
			
		}
		
	}
	
}	



//==========PWM======================

//注：不能仿真11位PWM，需要烧实际芯片测试
//需要先设置时钟，再针对单个PWM进行设置
// void	LPWMG1_PWM(void)
// {
// // 	$ LPWMGCLK Enable,/1,SYSCLK;	//是否启用LPWMG（写Enable启用，不写关闭），分频，时钟源选择
// // 								//分频可以选择/1, /2, /4, /8, /16, /32, /64, /128，时钟源可以选择SYSCLK,IHRC,IHRC*2（在code option中设置PWM_Source）
// // 								//打开LPWMG，LPWMG0、LPWMG1、LPWMG2共用同一个频率
// // //	$ LPWMGCLK ;					//关闭LPWMG，此时LPWMG0、LPWMG1、LPWMG2都关闭
// // 	LPWMGCUBL = 0b11_000000;		//上限低位寄存器：仅高2位（LPWMG0CUBL[7:6] 对应 CB10_1[2:1]）
// // 	LPWMGCUBH = 0b0001_1000;		//上限高位寄存器（对应CB10_1[10:3]）

// // 		//	LPWMG的频率的计算
// // 			//频率=时钟源/[分频*(CB10_1+1)]=1M / [1 * (0b1100011 + 1)] = 10000Hz

// 	$ LPWMGCLK Enable,/8,IHRC;
// 	$ LPWMG1C LPWMG1,LPWMG1_IO;
// 	LPWMG1DTL = 0b111_00000;
// 	LPWMG1DTH = 0b0111_1100;
// 	LPWMGCUBL = 0b11_000000;
// 	LPWMGCUBH = 0b1111_1001;

// }


//注：不能仿真11位PWM，需要烧实际芯片测试
//需要先设置时钟，再针对单个PWM进行设置
void	Close_LPWMG1(void)
{
// 	$ LPWMGCLK Enable,/1,SYSCLK;	//是否启用LPWMG（写Enable启用，不写关闭），分频，时钟源选择
// 								//分频可以选择/1, /2, /4, /8, /16, /32, /64, /128，时钟源可以选择SYSCLK,IHRC,IHRC*2（在code option中设置PWM_Source）
// 								//打开LPWMG，LPWMG0、LPWMG1、LPWMG2共用同一个频率
// //	$ LPWMGCLK ;					//关闭LPWMG，此时LPWMG0、LPWMG1、LPWMG2都关闭
// 	LPWMGCUBL = 0b11_000000;		//上限低位寄存器：仅高2位（LPWMG0CUBL[7:6] 对应 CB10_1[2:1]）
// 	LPWMGCUBH = 0b0001_1000;		//上限高位寄存器（对应CB10_1[10:3]）

// 		//	LPWMG的频率的计算
// 			//频率=时钟源/[分频*(CB10_1+1)]=1M / [1 * (0b1100011 + 1)] = 10000Hz
	$ LPWMGCLK /8,IHRC;

}

void	TM2_PWM(byte Duty)
{
	TM2CT = 0;					//计数寄存器
	TM2B = Duty;					//上限寄存器 
	$ TM2C SYSCLK,TIM2_PWM_IO,PWM;		//选择时钟源，输出脚，PWM模式，是否反极性输出（写Inverse为启用，不写则为停用）
								//根据要求时钟可选择SYSCLK, EOSC, IHRC, ILRC等，输出脚可以选择Disable（不选择）, PB2, PB4, PA3
								//注：时钟源与输出脚位的选择请参考对应芯片的datasheet，个别芯片有些不同



	//	$ TM2C STOP;				//关掉时钟，即TM2停止工作
	

	$ TM2S 8BIT,/1,/2;			//选择分辨率，预分频，分频
								//分辨率可选择8bit,6bit，预分频可选择/1, /4, /16, /64，分频可选择/1 ~ /32（对应TM2S[4:0]的00000 ~ 11111）

		//	PWM模式的频率和占空比计算
			//频率=时钟源/(分辨率*预分频*分频)=1M / (2^8 * 1 * 1) = 3906.25Hz
			//频率=时钟源/(分辨率*预分频*分频)=0.5M / (2^8 * 1 * 1) = 1953.125Hz
			//频率=时钟源/(分辨率*预分频*分频)=1M / (2^8 * 1 * 2) = 1953.125Hz

			//占空比=(TM2B+1)/分辨率*100%=(127 + 1) / 2^8 *100% = 50%
			//占空比=(TM2B+1)/分辨率*100%=(127 + 1) / 2^8 *100% = 60%   //153.6
			//占空比=(TM2B+1)/分辨率*100%=(127 + 1) / 2^8 *100% = 80%	//204.8
			//占空比=(TM2B+1)/分辨率*100%=(127 + 1) / 2^8 *100% = 100%	//256

}





//除脚位不同，其他原理和TM2一样
void	TM3_PWM(byte Duty)
{
	TM3CT = 0;
	TM3B = 	Duty;
	$ TM3C SYSCLK,TIM3_PWM_IO,PWM;		//输出脚可选择Disable（不选择）,PB5, PB6, PB7
								//注：时钟源与输出脚位的选择请参考对应芯片的datasheet，个别芯片有些不同

//	$ TM3C STOP;
	$ TM3S 8BIT,/1,/2;
}

//================================






//========T16中断设置========
void	T16_init(void)
{

	// $ T16M SYSCLK,/16,bit10;	//T16的时钟源选择，内部的时钟分频器，中断源选择（当选择位由低到高或者由高到低时，发生中断事件）；
								//时钟源选择可以选择STOP, SYSCLK, PA4_F, IHRC, EOSC, ILRC, PA0_F；分频器可选择/1, /4, /16, /64
								//中断源可选择BIT8, BIT9, BIT10, BIT11, BIT12, BIT13, BIT14, BIT15
	// reload_T16 = 1024 - 625;	//每次进中断为10ms；

	$ T16M SYSCLK,/1,bit15;

	reload_T16 = 32768 - 10000;;	//每次进中断为10ms；

				//计算公式为	[1/(时钟源/分频器)]*(中断源-reload_T16)=[1/(1M/16)]*(2^10-(1024-625))=0.01s

	stt16 reload_T16;			//设定计数器初始值reload_T16，当计数器累加超过设定中断源时产生中断;
	$ INTEGS BIT_R;				//T16中断边缘选择，上升缘请求中断为BIT_R，下降缘请求中断为BIT_F；默认为上升缘请求。
	//使用下面两句时，会关闭其他中断的设置，一般推荐使用对位操作的方法来驱动
//	$ INTEN T16;				//中断允许寄存器，启用从T16的溢出中断；1：启用，0：停用。
//	$ INTRQ T16;				//中断请求寄存器，此位是由硬件置位并由软件清零；1：请求，0：不请求。

	INTEN.T16 = 1;				//中断允许寄存器，开T16中断
//	INTEN.T16 = 0;				//中断允许寄存器，关T16中断
	INTRQ.T16 = 0;				//中断请求寄存器，清零INTRQ寄存器。

}



/*

0x30 --> 0
0x31 --> 1
0x32 --> 2
0x33 --> 3
0x34 --> 4
0x35 --> 5
0x36 --> 6
0x37 --> 7
0x38 --> 8
0x39 --> 9
0x3A --> :  待机模式

*/



void Key_Handle(void)
{

	//关机模式
	if(WorkMode == 0)
	{
		if(ShortKeyFlag_1)
		{
			ShortKeyFlag_1 = 0;
			// UART_Send_Data(KEY_1_SHORT_LOG,WorkMode);
#if DEBUG_EN
			UART_Send_Byte(KEY_1_SHORT_LOG);
#endif

		}

		if(ShortKeyFlag_2)
		{
			ShortKeyFlag_2 = 0;
			// UART_Send_Data(KEY_2_SHORT_LOG,WorkMode);
#if DEBUG_EN
			UART_Send_Byte(KEY_2_SHORT_LOG);
#endif

		}


		if(LongKeyFlag_1)
		{
			LongKeyFlag_1 = 0;
			$ LED_1 out,high;		//锟截碉拷
			$ LED_2 out,high;		//锟截碉拷
			$ LED_3 out,high;		//锟截碉拷
			$ LED_4 out,high;		//锟截碉拷

			WorkMode = 0x0A; //待机模式
			RF_InitFg = 1;
			// UART_Send_Data(KEY_1_LONG_LOG,WorkMode);
#if DEBUG_EN
			UART_Send_Byte(KEY_1_LONG_LOG);
#endif

		}

		if(LongKeyFlag_2)
		{
			LongKeyFlag_2 = 0;
			// UART_Send_Data(KEY_2_LONG_LOG,WorkMode);
#if DEBUG_EN
			UART_Send_Byte(KEY_2_LONG_LOG);
#endif


		}


	}
	else
	{

		if(ShortKeyFlag_1)
		{
			ShortKeyFlag_1 = 0;
			// UART_Send_Data(KEY_1_SHORT_LOG,WorkMode);
#if DEBUG_EN
			UART_Send_Byte(KEY_1_SHORT_LOG);
#endif

		}

		if(ShortKeyFlag_2)
		{
			ShortKeyFlag_2 = 0;

			PwmOpenFlag = 0;  				 //1:open 2:close
			ModeSegFlag = 0; 				 //0:模式0 1:模式1
			ModeSegOKFg = 0;				 //模式切换标志 1：需要切换  0：切换完成	
			ModeSegCnt = 0;					 //间隔
			PwmShockDutyCnt = 37;			 //模式7 震动变化占空比		
			PwmPatDutyCnt = 37;				//模式7 拍打变化占空比		
			OpenPWMCntms = 0;    			/* 开PWM */
			ClosePWMCntms = 0;   			/* 关PWM */

			switch(WorkMode)
			{

				case 1:
					WorkMode = 2;
					PwmOpenFlag = 1;
					TM2_PWM(204); 
					TM3_PWM(204);
					break;

				case 2:
					WorkMode = 3;
					PwmOpenFlag = 1;
					TM2_PWM(255);
					TM3_PWM(255);
					break;

				case 3:  //200ms模式3 50ms关闭
					WorkMode = 4;
					PwmOpenFlag = 1;
					TM2_PWM(0);
					TM3_PWM(0);

					break;

				case 4:
					WorkMode = 5;
					PwmOpenFlag = 1;
					TM2_PWM(255);
					TM3_PWM(255);

					break;

				case 5:
					WorkMode = 6;
					PwmOpenFlag = 1;
					TM2_PWM(0);
					TM3_PWM(0);

					break;

				case 6:
					WorkMode = 7;
					PwmOpenFlag = 1;
					TM2_PWM(PwmShockDutyCnt);
					TM3_PWM(PwmShockDutyCnt);

					break;

				case 7:
					WorkMode = 8;
					PwmOpenFlag = 1;
					TM2_PWM(37);
					TM3_PWM(37);

					break;

				case 8:
					WorkMode = 9;
					PwmOpenFlag = 1;
					TM2_PWM(37);
					TM3_PWM(37);

					break;

				case 9:
					WorkMode = 1;
					PwmOpenFlag = 1;
					TM2_PWM(153); 
					TM3_PWM(153);
					break;

				case 10:
					WorkMode = 1;
					PwmOpenFlag = 1;
					TM2_PWM(153); 
					TM3_PWM(153);
					break;

				default:
					WorkMode = 10;
					break;
			}

			// UART_Send_Data(KEY_1_SHORT_LOG,WorkMode);
#if DEBUG_EN
			UART_Send_Byte(KEY_1_SHORT_LOG);
#endif

		}

		if(LongKeyFlag_2)
		{
			LongKeyFlag_2 = 0;
			WorkMode = 10;
			PwmOpenFlag = 0;
			TM2_PWM(0);
			TM3_PWM(0);
			// UART_Send_Data(KEY_2_LONG_LOG,WorkMode);
#if DEBUG_EN
			UART_Send_Byte(KEY_2_LONG_LOG);
#endif

		}


		if(LongKeyFlag_1)
		{
			LongKeyFlag_1 = 0;

			TM2_PWM(0);
			TM3_PWM(0);
			$ LED_1 out,low;		//锟截碉拷
			$ LED_2 out,low;		//锟截碉拷
			$ LED_3 out,low;		//锟截碉拷
			$ LED_4 out,low;		//锟截碉拷

			WorkMode = 0;

			// UART_Send_Data(KEY_1_LONG_LOG,WorkMode);
#if DEBUG_EN
			UART_Send_Byte(KEY_2_SHORT_LOG);
#endif


		}



	}


}

/*

0x30 --> 0
0x31 --> 1
0x32 --> 2
0x33 --> 3
0x34 --> 4
0x35 --> 5
0x36 --> 6
0x37 --> 7
0x38 --> 8
0x39 --> 9
0x3A --> :  待机模式

*/

void JudgeMode_Receive(uint8_t received_mode)
{
	
	PwmOpenFlag = 0;  				 //1:open 2:close
	ModeSegFlag = 0; 				 //0:模式0 1:模式1
	ModeSegOKFg = 0;				 //模式切换标志 1：需要切换  0：切换完成	
	ModeSegCnt = 0;					 //间隔
	PwmShockDutyCnt = 37;			 //模式7 震动变化占空比		
	PwmPatDutyCnt = 37;				//模式7 拍打变化占空比		
	OpenPWMCntms = 0;    			/* 开PWM */
	ClosePWMCntms = 0;   			/* 关PWM */

	switch(received_mode)
	{

		case 0x31:
			WorkMode = 1;
			PwmOpenFlag = 1;
			TM2_PWM(153); 
			TM3_PWM(153);
			break;

		case 0x32:
			WorkMode = 2;
			PwmOpenFlag = 1;
			TM2_PWM(204); 
			TM3_PWM(204);
			break;

		case 0x33:
			WorkMode = 3;
			PwmOpenFlag = 1;
			TM2_PWM(255);
			TM3_PWM(255);
			break;

		case 0x34:  //200ms模式3 50ms关闭
			WorkMode = 4;
			PwmOpenFlag = 1;
			TM2_PWM(0);
			TM3_PWM(0);

			break;

		case 0x35:
			WorkMode = 5;
			PwmOpenFlag = 1;
			TM2_PWM(255);
			TM3_PWM(255);

			break;

		case 0x36:
			WorkMode = 6;
			PwmOpenFlag = 1;
			TM2_PWM(0);
			TM3_PWM(0);

			break;

		case 0x37:
			WorkMode = 7;
			PwmOpenFlag = 1;
			TM2_PWM(PwmShockDutyCnt);
			TM3_PWM(PwmShockDutyCnt);

			break;

		case 0x38:
			WorkMode = 8;
			PwmOpenFlag = 1;
			TM2_PWM(37);
			TM3_PWM(37);

			break;

		case 0x39:
			WorkMode = 9;
			PwmOpenFlag = 1;
			TM2_PWM(37);
			TM3_PWM(37);

			break;

		case 0x3A:
			WorkMode = 10;
			PwmOpenFlag = 0;
			TM2_PWM(0);
			TM3_PWM(0);

			break;


		default:
			WorkMode = 10;
			PwmOpenFlag = 0;
			TM2_PWM(0);
			TM3_PWM(0);

			break;
	}

#if DEBUG_EN
	UART_Send_Byte(WorkMode);
#endif


}




void JudgeMode_While(void)
{
	//======User can add code=====
	//============================

	if(ChargingStatus)
	{
		if(1 == ModeSegOKFg) //
		{
			ModeSegOKFg = 0;
			TM2_PWM(0);
			TM3_PWM(0);	
			$ LED_1 out,low;		//锟截碉拷
			$ LED_2 out,low;		//锟截碉拷
			$ LED_3 out,low;		//锟截碉拷
			$ LED_4 out,low;		//锟截碉拷

		}
	}
	else
	{
		switch(WorkMode)
		{
			case 0:
				if(1 == ModeSegOKFg) //
				{
					ModeSegOKFg = 0;
					TM2_PWM(0);
					TM3_PWM(0);	
				}

				break;

			case 1:

				break;

			case 2:

				break;

			case 3:  

				break;

			case 4://200ms模式3 50ms关闭
				if(1 == PwmOpenFlag)
				{
					if(1 == ModeSegOKFg) //
					{
						ModeSegOKFg = 0;

						if(0 == ModeSegFlag)
						{
							TM2_PWM(0);
							TM3_PWM(0);	
						}
						else 
						{
							TM2_PWM(255);
							TM3_PWM(255);	
						}
					}
					
				}
				break;

			case 5:
				if(1 == PwmOpenFlag)
				{
					if(1 == ModeSegOKFg) //
					{
						ModeSegOKFg = 0;
						TM2_PWM(255);
						TM3_PWM(255);

					}
					
				}
				else
				{
					if(1 == ModeSegOKFg) //
					{
						ModeSegOKFg = 0;
						TM2_PWM(0);
						TM3_PWM(0);	
					}
				}
				break;

			case 6:
				if(1 == PwmOpenFlag)
				{
					if(1 == ModeSegOKFg) //
					{
						ModeSegOKFg = 0;

						if(0 == ModeSegFlag)
						{
							TM2_PWM(0);
							TM3_PWM(0);	
						}
						else 
						{
							TM2_PWM(255);
							TM3_PWM(255);	
						}
					}
					
				}
				break;

			case 7:
				if(1 == PwmOpenFlag)
				{
					if(1 == ModeSegOKFg) //
					{
						ModeSegOKFg = 0;
						TM2_PWM(PwmShockDutyCnt);
						TM3_PWM(PwmShockDutyCnt);	

					}
					
				}
				break;

			case 8:
				if(0 == ModeSegFlag)
				{
					if(1 == PwmOpenFlag)
					{
						if(1 == ModeSegOKFg) //
						{
							ModeSegOKFg = 0;
							TM2_PWM(37);
							TM3_PWM(37);

						}
						
					}
					else
					{
						if(1 == ModeSegOKFg) //
						{
							ModeSegOKFg = 0;
							TM2_PWM(255);
							TM3_PWM(255);	
						}
					}
					
				}
				else
				{
					if(1 == PwmOpenFlag)
					{
						if(1 == ModeSegOKFg) //
						{
							ModeSegOKFg = 0;
							TM2_PWM(37);
							TM3_PWM(37);

						}
						
					}
					else
					{
						if(1 == ModeSegOKFg) //
						{
							ModeSegOKFg = 0;
							TM2_PWM(255);
							TM3_PWM(255);	
						}
					}
				}

				break;

			case 9:
				if(1 == PwmOpenFlag)
				{
					if(0 == ModeSegFlag)
					{
						if(1 == ModeSegOKFg) //
						{
							ModeSegOKFg = 0;
							TM2_PWM(37);
							TM3_PWM(37);

						}
					}
					else
					{
						if(1 == ModeSegOKFg) //
						{
							ModeSegOKFg = 0;
							TM2_PWM(255);
							TM3_PWM(255);	
						}

					}

				}


				break;

			case 10:
				if(1 == ModeSegOKFg) //
				{
					ModeSegOKFg = 0;
					TM2_PWM(0);
					TM3_PWM(0);	
				}
				break;

			default:

				break;

		}


	}

	



}

// void receive_parameters_init(void)
// {
//   	RX_len = 0;
// 	RxData = 0;
// 	RxCnt = 0;
// 	data_len = 0;
// 	blecrc = 0;
// 	calc_crc = 0;
// 	RX_i = 0;
// 	RX_j = 0;
// 	RX_k = 0;
// 	RX_cfg_val = 0;

// }




// byte Get_APP_CheckNum(void)
// {
// 	uint8_t i = 13;
// 	uint32_t sum = 0;

// 	while (i < 22)
// 	{
// 		sum += RXbufData[i];
// 		i++;
// 		/* code */
// 	}
	

// 	return (sum & 0xFF);


// }



void Send_Mode_Fun(void)
{
	if(SendPackageCnt)
	{
		rf_setchannel_tx();
		rf_adv_data_config();
		rf_ble_crc_tx();
		rf_ble_whiten_tx();
		RFAddr = FLUSH_TX;
		RFWData = CMD_NOP;
		RF_WriteReg();
		rf_tx_packet();
		SendPackageCnt--;
		// UART_Send_Byte(SendPackageCnt);
	}
	else
	{
		if(FirstRxFg == 1)
		{
			FirstRxFg = 2;
		}	

		RF_InitFg = 1;
		Trans_Mode = RX_MODE; // 发送完切换接收模式
	}

}



void Receive_Mode_Fun(void)
{

		uint8_t RxData = 0;
		uint8_t Rx_i = 0;
		uint8_t Rx_j = 0;
		uint8_t data_len = 0;
		uint32_t blecrc = 0;
		uint32_t calc_crc = 0;
		uint8_t RX_k = 0;

		RX_len = rf_rx_packet();

			if(DEBUG_1ms_Cnt >= TASK_1S)		//
			{
				DEBUG_1ms_Cnt = 0;
				// UART_Send_Byte(0xAA);
				// .printf("%x\r\n",RX_len);
			}

			if (RX_len)
			{
				// UART_Send_Byte(RX_len);

				if (RX_len > BUFF_LEN)
				{
					// rf_spi_write_Reg(FLUSH_RX, CMD_NOP);
					RFAddr = FLUSH_RX;
					RFWData = CMD_NOP;
					RF_WriteReg();
					RX_len = 0;
					return;
				}


				while (Rx_i < RX_len)
				{
					RxData = rf_reversebits(RXbufData[Rx_i]);
					RXbufData[Rx_i] = RxData;
					Rx_i++;

				}

				rf_ble_whiten_rx();

				while (Rx_j < RX_len)
				{
					RxData = rf_reversebits(RXbufData[Rx_j]);
					RXbufData[Rx_j] = RxData;
					Rx_j++;
				}

	#if DEBUG_EN
				Rx_i = 0;
				UART_Send_Byte(RX_len);
				while (Rx_i < RX_len)
				{
					UART_Send_Byte(RXbufData[Rx_i]);
					Rx_i++;
				}
#endif	



				//先判断包长和固定字段，合法包再计算CRC，减少无效包运算
				if( (RX_len == BUFF_LEN) &&
					(RXbufData[0] == 0x42) &&
					(RXbufData[1] == 0x25) &&
					(RXbufData[13] == 0x4C) &&
					(RXbufData[14] == 0x58) 
					/*&&
					(RXbufData[15] == 0x01) &&
					(RXbufData[16] == 0x01) &&
					(RXbufData[17] == 0x01) &&
					(RXbufData[18] == 0x00) &&
					(RXbufData[19] == 0x00) &&
					(RXbufData[20] == 0x01) */
				)
				{
			
					if (RXbufData[1] > (BUFF_LEN - 5))
					{
						data_len = BUFF_LEN - 5;

					}
					else
					{
						data_len = RXbufData[1];

					}           


					blecrc = (RXbufData[data_len + 5 - 1] << 16) + (RXbufData[data_len + 5 - 2] << 8) + RXbufData[data_len + 5 - 3];

					RXbufData[data_len + 5 - 3] = 0x55;
					RXbufData[data_len + 5 - 2] = 0x55;
					RXbufData[data_len + 5 - 1] = 0x55;
					
					rf_ble_crc_rx(data_len + 5 - 3);

					calc_crc |= RXbufData[data_len + 5 - 1] << 16;
					calc_crc |= RXbufData[data_len + 5 - 2] << 8;
					calc_crc |= RXbufData[data_len + 5 - 3];
#if DEBUG_EN
					UART_Send_Byte(blecrc);
					UART_Send_Byte(calc_crc);
#endif
					if(blecrc == calc_crc)
					{

						if(TempWorkMode != RXbufData[21])
						{
							if(FirstRxFg == 0)  //第一次收到
							{
								FirstRxFg = 1;
								RF_InitFg = 1;
								Trans_Mode = TX_MODE;		//切换发送模式
								SendPackageCnt = SENDPACKCNT;
							}
							TempWorkMode = RXbufData[21];
							JudgeMode_Receive(TempWorkMode);
						}
					}
				}
				while(RX_k < BUFF_LEN)  // 
				{
					RXbufData[RX_k] = 0x00;
					RX_k++;
				}
			
				RX_len = 0;

				// CE_CTL_HIGH;
				// // ==================== CE_HIGH 展开实现 ====================
				// RX_cfg_val = rf_spi_read_reg(CFG_TOP);       // 读取CFG_TOP寄存器
				// RX_cfg_val |= 0x01;                      // 第0位置1
				// rf_spi_write_Reg(W_REG | CFG_TOP, RX_cfg_val);
				CE_High();

			}	


}





//=======低功耗=========


//=======掉电模式=========
void	Power_down(void)
{
	// uint8_t TempVolue = 0;		//
	//======User can add code=====
	//进入省电模式前动作，如关灯、关计数器等
	//============================
	DISGINT;		//关闭全局中断
	// INTEN.T16 = 1;				//中断允许寄存器，开T16中断
	INTEN.T16 = 0;				//中断允许寄存器，关T16中断
	// INTRQ.T16 = 0;				//中断请求寄存器，清零INTRQ寄存器。
	rf_sleep_reset();
	FirstRxFg = 0;

	LowPower_CLKMD_BK = CLKMD;					//保存休眠前的时钟
	PADIER = 0b0000_0000;				//将PA0设置为数字模式
	PBDIER = 0b0010_0001;
	PCDIER = 0b0000_0000;
	$ RPIN_IO in,pull;//
	$ KEY_1_IO in,pull;//
	$ LOW_P_IO out,high;		//锟截碉拷

	//休眠前需要切换低频ILRC用来防止唤醒失败
	$ CLKMD ILRC/1,En_IHRC,En_ILRC;		//系统时钟选择，是否启用IHRC，是否启用ILRC，（En_IHRC和En_ILRC不写为停用，写为启用）
										//系统时钟可选择IHRC/4, IHRC/16, IHRC/2, IHRC/8, ILRC/16, EOSC/4, IHRC/32, EOSC/2, IHRC/64, EOSC/1, EOSC/8, ILRC/4, ILRC/1
										//选择系统时钟为ILRC/1，启用ILRC和IHRC；（注：两个RC振荡器至少有一个开启，否则会出现宕机）
	CLKMD.En_IHRC = 0;					//关闭高频IHRC，若上条语句没使用低频时钟，此句必须去掉
	nop;
	while(1)
	{
		stopsys;						//进入断电模式
	//=======编写唤醒条件=========
		//例如PA0由高变低唤醒，该方法针对IO脚电平变化的唤醒条件
		if((KEY_1_IO == 0) || (RPIN_IO == 1)) 						//假如发生唤醒而且检查OK，就返回正常工作
		{									//否则停留在断电模式
			LowPower_1ms_Cnt = 0;	
			break;
		}
		
	}
	$ CLKMD ILRC/1,EN_IHRC,EN_ILRC;		//打开高频使能，准备切换回系统时钟
	nop;
	CLKMD = LowPower_CLKMD_BK;					//恢复休眠前的时钟
	ENGINT;			//开启全局中断

	//SPI:PA7,PA6,PA3
	//key_1：PB4
	//key_2：PA4
	//未使用端口需有防漏电设置，如加内部上拉
	PADIER 	= 	0b1101_1000;				//将PA0设置为数字模式
	PBDIER  = 	0b0010_0001;

	.delay 1000;
	rf_spi_init();
	rf_wakeup();

	//======User can add code=====
	//唤醒后打开需要的动作，比如开灯、定时器等
	//============================	
	INTEN.T16 = 1;				//中断允许寄存器，开T16中断
	// INTEN.T16 = 0;				//中断允许寄存器，关T16中断
	INTRQ.T16 = 0;				//中断请求寄存器，清零INTRQ寄存器。
	// Key_init();

	$ LOW_P_IO out,low;		//锟截碉拷
	$ RPIN_IO in,pull;//
	$ CHRG_IO in,pull;//

	// TempVolue = rf_spi_read_reg(CFG_TOP);       // 读取CFG_TOP寄存器

	// UART_Send_Data(LOG_Ing,TempVolue);	

	// rf_adv_init();
	// rf_rx_init();
	// rf_wakeup();
	// rf_wakeup();
	RF_InitFg = 1;

}



void	FPPA0 (void)
{
	.ADJUST_IC	SYSCLK=IHRC/16, IHRC=16MHz, VDD=3.3V,Init_ram;
	// .ADJUST_IC	SYSCLK=IHRC/16, IHRC=16MHz, VDD=3.3V;

	SYS_CLKMD = CLKMD;	//初始记录系统时钟，在UART通讯后方便切回系统时钟
	$ UART_Out High,Out;//设置UART的通讯脚（发送信号）

	//SPI:PA7,PA6,PA3
	//key_1：PB4
	//key_2：PA4
	//未使用端口需有防漏电设置，如加内部上拉
	PADIER 	= 	0b1101_1000;				//将PA0设置为数字模式
	PBDIER  = 	0b0010_0001;
	
	TM2_PWM(0);
	TM3_PWM(0);
	ADC_init();
	Key_init();
	rf_spi_init();
	T16_init();

	$ LOW_P_IO out,low;		//锟截碉拷
	$ RPIN_IO in,pull;//
	$ CHRG_IO in,pull;//


	rf_adv_channel[0] = RF_ADV_CHANNEL_37;
	rf_adv_channel[1] = RF_ADV_CHANNEL_38;
	rf_adv_channel[2] = RF_ADV_CHANNEL_39;


//========中断设置========
	ENGINT;			//开启全局中断
//	DISGINT;		//关闭全局中断
	.delay 1000;		//等待

#if DEBUG_EN
	UART_Send_Byte(0x55);
#endif
	// .printf(" 123 \r\n");

	while (1)
	{
		if(LOG_1ms_Cnt >= TASK_1S)		//
		{

			LOG_1ms_Cnt = 0;

			// UART_Send_Byte(RF_InitFg);	
			// UART_Send_Byte(ChargingStatus);	

			// UART_Send_Byte(WorkMode);	
			// UART_Send_Byte(Trans_Mode);	

			// .printf("%x\r\n",WorkMode);
			// .printf("%x\r\n",RF_WorkMode);
			// .printf("%x\r\n",Trans_Mode);

			//ADC数据发送
			// Log_Buf_Out[0] = PC2_AD_Data >> 8;
			// Log_Buf_Out[1] = PC2_AD_Data & 0xFF;
			// UART_HandShake(AD_LOG,Log_Buf_Out,2);

		}

		if((WorkMode != 0)&&(ChargingStatus == 0))
		{
			if(RF_InitFg == 1) 
			{
				RF_InitFg = 0;
				INTEN.T16 = 0;				//中断允许寄存器，关T16中断
				rf_adv_init();
				rf_rx_init();
				INTEN.T16 = 1;				//中断允许寄存器，开T16中断
				INTRQ.T16 = 0;				//中断请求寄存器，清零INTRQ寄存器。
			}

			//接收模式
			if(Trans_Mode == RX_MODE) 
			{
				Receive_Mode_Fun();
			}
			//发送模式	
			else	
			{
				Send_Mode_Fun();
			}

		}
		

		//按键2
		if((KeyFlag_2 == 0) && (ChargingStatus == 0))
		{
			KeyFlag_2 = 1;  //
		}

		// 充电判断
		if((ChargingStatus == 0) && (RPIN_IO == 1))
		{
			ChargingStatus = 1;
			WorkMode = 0;
			// UART_Send_Byte(ChargingStatus);
		}
		else if ((ChargingStatus != 0) && (RPIN_IO == 0))
		{
			ChargingStatus = 0;
			WorkMode = 0;
			TM2_PWM(0);
			TM3_PWM(0);
			$ LED_1 out,low;		//锟截碉拷
			$ LED_2 out,low;		//锟截碉拷
			$ LED_3 out,low;		//锟截碉拷
			$ LED_4 out,low;		//锟截碉拷
			
			// UART_Send_Byte(ChargingStatus);

		}
		else if( (ChargingStatus == 1) && 
				(RPIN_IO == 1) && 
				((CHRG_IO == 1) ||
				(PC2_AD_Data >= 0xB11)))
		{
			ChargingStatus = 2;
			// UART_Send_Byte(ChargingStatus);

		}


		if(ADC_1ms_Cnt >= TASK_20MS)		//
		{
			ADC_1ms_Cnt = 0;
			ADC_mea();
			// Median_ave();

		}

		Key_Handle();
		JudgeMode_While();

		// //充电状态下的呼吸灯
		if(ChargingStatus == 1)
		{
			if(BlinkFg)
			{
				$ LED_1 out,high;		//锟截碉拷
				$ LED_2 out,high;		//锟截碉拷
				$ LED_3 out,high;		//锟截碉拷
				$ LED_4 out,high;		//锟截碉拷

			}	
			else
			{
				$ LED_1 out,low;		//锟截碉拷
				$ LED_2 out,low;		//锟截碉拷
				$ LED_3 out,low;		//锟截碉拷
				$ LED_4 out,low;		//锟截碉拷
			}
		}
		else if(ChargingStatus == 2)
		{
			$ LED_1 out,high;		//锟截碉拷
			$ LED_2 out,high;		//锟截碉拷
			$ LED_3 out,high;		//锟截碉拷
			$ LED_4 out,high;		//锟截碉拷
		}


		// 低电
		if((PC2_AD_Data <= 0x889) &&
			(PC2_AD_Data > 0))		
		{
			WorkMode = 0;

			TM2_PWM(0);
			TM3_PWM(0);
			$ LED_1 out,low;		//锟截碉拷
			$ LED_2 out,low;		//锟截碉拷
			$ LED_3 out,low;		//锟截碉拷
			$ LED_4 out,low;		//锟截碉拷

			// UART_Send_Byte(AD_LOG);

		}

		// 低功耗
		if( (LowPower_1ms_Cnt >= TASK_3S) &&
			(WorkMode == 0) && 
			(ChargingStatus == 0) )
		{
			Power_down();

		}

	}


}


void	Interrupt (void)
{
	pushaf;

	if (Intrq.T16)
	{	//	T16 Trig
		//	User can add code
		stt16 reload_T16;		//设定计数器初始值reload_T16
		Time_1ms_Cnt++;
		ADC_1ms_Cnt++;
		LOG_1ms_Cnt++;
		DEBUG_1ms_Cnt++;

		if((WorkMode == 0) &&
			(ChargingStatus == 0))
		{
			LowPower_1ms_Cnt++;
		}
		else
		{
			LowPower_1ms_Cnt = 0;
		}	


		if(ChargingStatus)
		{
			BlinkCnt++;
			if(BlinkCnt >= 50)
			{
				BlinkCnt = 0;
				if(BlinkFg)
				{
					BlinkFg = 0;
				}	
				else
				{
					BlinkFg = 1;
				}

			}	

		}
		else
		{
			BlinkCnt = 0;
			BlinkFg = 0;
		}


		if(FirstRxFg == 2)  //稳定接收
		{
			//接收模式5S切换为发送模式
			if(Trans_Mode == RX_MODE)
			{
				Connect_1ms_Cnt++;
				if(Connect_1ms_Cnt >= TASK_5S)
				{
					Connect_1ms_Cnt = 0;
					RF_InitFg = 1;
					Trans_Mode = TX_MODE;		//切换发送模式
					SendPackageCnt = SENDPACKCNT;
				}

			}
		}


		KeyJudge_1();
		KeyJudge_2();
		JudgeMode_Timer();
		
		Intrq.T16 = 0;

	}



//=========PB0===========
	if (Intrq.PB0 && Inten.PB0)
	{
		Intrq.PB0 = 0;
		if((KeyFlag_1 == 0) && (ChargingStatus == 0))
		{
			KeyFlag_1 = 1;  //
		}

		//...
	}


	popaf;

}
